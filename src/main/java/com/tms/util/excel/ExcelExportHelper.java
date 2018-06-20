package com.tms.util.excel;


import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.util.CellRangeAddress;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


/**
 * excel导出,单个工作表,多个工作表导出
 * @author liukongshuo
 *
 */
public class ExcelExportHelper {
	public SimpleDateFormat timeFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	private HttpServletResponse response;

	private HttpSession session;

	private ServletOutputStream out;

	private InputStream downloadFile;

	/**
	 * 大数据量导出静态变量
	 */
	public static int EXCEL_MAX_CNT = 100000; //每个excel文件多少条数据
	public static int SHEET_MAX_CNT = 20000; //每个sheet多少条数据
	
	public static String SHEET_STYLE ="sheetStyle";
	public static String SHEET_HEADER_STYLE ="headerStyle";
	
	public ExcelExportHelper() {
	}

	public ExcelExportHelper(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		this.response = response;
		session = request.getSession();
		init(this.session);
	}

	private void init(HttpSession session) throws Exception {
		out = response.getOutputStream();
	}

	/**
	 * 向浏览器输出JSON数据
	 * 
	 * @param
	 * @return void
	 */
	public void outDataToBrowser(TableData tableData) {
		StringBuffer outData = new StringBuffer();

		// 向前台输出数据
		outData.append("{pageInfo: {totalRowNum: " + tableData.getTotalRows()
				+ "},");
		outData.append("data: [");
		boolean isFirst = true;

		TableHeaderMetaData headerMetaData = tableData.getTableHeader();
		List<TableDataRow> dataRows = tableData.getRows();
		try {
			for (TableDataRow dataRow : dataRows) {
				List<TableDataCell> dataCells = dataRow.getCells();
				int size = dataCells.size();
				if (!isFirst) {
					outData.append(",{");
					for (int i = 0; i < size; i++) {
						outData.append(headerMetaData.getColumnAt(i).getId()
								+ ": '" + dataCells.get(i).getValue() + "',");
					}
					int index = outData.lastIndexOf(",");
					outData.deleteCharAt(index);
					outData.append("}");
				} else {
					outData.append("{");
					for (int i = 0; i < size; i++) {
						outData.append(headerMetaData.getColumnAt(i).getId()
								+ ": '" + dataCells.get(i).getValue() + "',");
					}
					int index = outData.lastIndexOf(",");
					outData.deleteCharAt(index);
					outData.append("}");
					isFirst = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		outData.append("]");
		outData.append("}");

		try {
			out.print(outData.toString());
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param
	 * @return void
	 */
	private void stopGrouping(HSSFSheet sheet, HashMap<Integer, String> word,
                              HashMap<Integer, Integer> counter, int i, int size, int rownum,
                              HSSFCellStyle style) {
		String w = word.get(i);
		if (w != null) {
			int len = counter.get(i);
			CellRangeAddress address = new CellRangeAddress(rownum - len,
					rownum - 1, i, i);
			sheet.addMergedRegion(address);
			fillMergedRegion(sheet, address, style);
			word.remove(i);
			counter.remove(i);
		}
		if (i + 1 < size) {
			stopGrouping(sheet, word, counter, i + 1, size, rownum, style);
		}
	}

	/**
	 * 
	 * @param
	 * @return void
	 */
	private void generateColumn(HSSFSheet sheet, TableColumn tc, int maxlevel,
			int rownum, int colnum, HSSFCellStyle headerstyle) {
		HSSFRow row = sheet.getRow(rownum);
		if (row == null)
			row = sheet.createRow(rownum);

		HSSFCell cell = row.createCell(colnum);
		cell.setCellValue(tc.getDisplay());

		if (headerstyle != null)
			cell.setCellStyle(headerstyle);
		if (tc.isComplex()) {
			CellRangeAddress address = new CellRangeAddress(rownum, rownum,
					colnum, colnum + tc.getLength() - 1);
			sheet.addMergedRegion(address);
			fillMergedRegion(sheet, address, headerstyle);

			int cn = colnum;
			for (int i = 0; i < tc.getChildren().size(); i++) {
				if (i != 0) {
					cn = cn + tc.getChildren().get(i - 1).getLength();
				}
				generateColumn(sheet, tc.getChildren().get(i), maxlevel,
						rownum + 1, cn, headerstyle);
			}
		} else {
			CellRangeAddress address = new CellRangeAddress(rownum, rownum
					+ maxlevel - tc.level, colnum, colnum);
			sheet.addMergedRegion(address);
			fillMergedRegion(sheet, address, headerstyle);
		}
		sheet.autoSizeColumn(colnum, true);
	}

	/**
	 * 
	 * @param
	 * @return void
	 */
	private void fillMergedRegion(HSSFSheet sheet, CellRangeAddress address,
			HSSFCellStyle style) {
		for (int i = address.getFirstRow(); i <= address.getLastRow(); i++) {
			HSSFRow row = sheet.getRow(i);
			if (row == null)
				row = sheet.createRow(i);
			for (int j = address.getFirstColumn(); j <= address.getLastColumn(); j++) {
				HSSFCell cell = row.getCell(j);
				if (cell == null) {
					cell = row.createCell(j);
					if (style != null)
						cell.setCellStyle(style);
				}
			}
		}
	}
	
	/**
	 * 写入工作表
	 * @param wb Excel工作簿
	 * @param title Sheet工作表名称
	 * @param styles 表头样式
	 * @param tableData 表格数据
	 * @throws Exception
	 */
	public HSSFWorkbook writeSheet(HSSFWorkbook wb, String title, HashMap<String, HSSFCellStyle> styles, TableData tableData, int freezeColumnCount) throws Exception {

		TableHeaderMetaData headerMetaData = tableData.getTableHeader();// 获得HTML的表头元素
		
		HSSFSheet sheet = wb.createSheet(title);// 在Excel工作簿中建一工作表
		sheet.setDisplayGridlines(false);// 设置表标题是否有表格边框
		if(freezeColumnCount>0)
			sheet.createFreezePane(freezeColumnCount, 1, freezeColumnCount, 1);//设置表头行,固定表头列冻结

		HSSFRow row = null;// 创建行

		int rownum = 0;

		int colnum = 0;
		for (int i = 0; i < headerMetaData.getOriginColumns().size(); i++) {
			TableColumn tc = headerMetaData.getOriginColumns().get(i);
			if (i != 0) {
				colnum += headerMetaData.getOriginColumns().get(i - 1)
						.getLength();
			}
			generateColumn(sheet, tc, headerMetaData.maxlevel, rownum, colnum,
					styles.get(SHEET_HEADER_STYLE));
		}
		rownum += headerMetaData.maxlevel;

		List<TableDataRow> dataRows = tableData.getRows();

		HashMap<Integer, Integer> counter = new HashMap<Integer, Integer>();
		HashMap<Integer, String> word = new HashMap<Integer, String>();
		int index = 0;
		for (TableDataRow dataRow : dataRows) {
			row = sheet.createRow(rownum);

			List<TableDataCell> dataCells = dataRow.getCells();
			int size = headerMetaData.getColumns().size();
			index = -1;
			for (int i = 0; i < size; i++) {
				TableColumn tc = headerMetaData.getColumns().get(i);
				if (!tc.isVisible())
					continue;
				index++;

				String value = dataCells.get(i).getValue();
				if (tc.isGrouped()) {
					String w = word.get(index);
					if (w == null) {
						word.put(index, value);
						counter.put(index, 1);
						createCell(row, tc, dataCells, i, index, styles);
					} else {
						if (w.equals(value)) {
							counter.put(index, counter.get(index) + 1);
						} else {
							stopGrouping(sheet, word, counter, index, size,rownum, styles.get("STRING"));

							word.put(index, value);
							counter.put(index, 1);
							createCell(row, tc, dataCells, i, index, styles);
						}
					}
				} else {
					createCell(row, tc, dataCells, i, index, styles);
				}
			}
			rownum++;
		}

//		stopGrouping(sheet, word, counter, 0, index, rownum, styles.get("STRING"));
		// 设置前两列根据数据自动列宽
		for (int c = 0; c < headerMetaData.getColumns().size(); c++) {
			sheet.autoSizeColumn((short) c);
			String t = headerMetaData.getColumns().get(c).getDisplay();
			if(sheet.getColumnWidth(c)<t.length()*256*3)
				sheet.setColumnWidth(c, t.length()*256*3);
			if(sheet.getColumnWidth(c)>t.length()*256*10)
				sheet.setColumnWidth(c, t.length()*256*10);
		}
		sheet.setGridsPrinted(true);
		
		sheet.getRow(0).setHeight((short)(256*4));//设置头部高度
		return wb;
	}
	
	/**
	 * 写入工作表
	 * @param wb Excel工作簿
	 * @param styles 表头样式
	 * @param creator 创建人
	 * @throws Exception
	 */
	public HSSFWorkbook writeSheet(HSSFWorkbook wb, HashMap<String, HSSFCellStyle> styles, String creator, List<TableData> tableDataLst) throws Exception {
		
		SimpleDateFormat formater = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分");
		String create_time = formater.format(new Date());
		
		int cnt = 1;
		for(TableData tableData : tableDataLst){
			String sheetTitle = tableData.getSheetTitle();
			sheetTitle = sheetTitle==null||sheetTitle.equals("")?"sheet"+cnt:sheetTitle;
			cnt++;
	
			TableHeaderMetaData headerMetaData = tableData.getTableHeader();// 获得HTML的表头元素
			HSSFSheet sheet = wb.createSheet(sheetTitle);// 在Excel工作簿中建一工作表
			sheet.setDisplayGridlines(false);// 设置表标题是否有表格边框
			wb.cloneSheet(0);
			
			//创建标题
			HSSFRow row = sheet.createRow(0);// 创建新行
			HSSFCell cell = row.createCell(0);// 创建新列
			int rownum = 0;
			cell.setCellValue(new HSSFRichTextString(sheetTitle));
			HSSFCellStyle style = styles.get("TITLE");//设置标题样式
			if (style != null)
				cell.setCellStyle(style);
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headerMetaData
					.getColumnCount() - 1));//合并标题行：起始行号，终止行号， 起始列号，终止列号
			
			//创建副标题
			row = sheet.createRow(1);
			cell = row.createCell(0);
			cell.setCellValue(new HSSFRichTextString("创建人:"));
			style = styles.get("SUB_TITLE");
			if (style != null)
				cell.setCellStyle(style);
			
			cell = row.createCell(1);
			cell.setCellValue(new HSSFRichTextString(creator));
			style = styles.get("SUB_TITLE2");
			if (style != null)
				cell.setCellStyle(style);
			
			cell = row.createCell(2);
			cell.setCellValue(new HSSFRichTextString("创建时间:"));
			style = styles.get("SUB_TITLE");
			if (style != null)
				cell.setCellStyle(style);
			
			cell = row.createCell(3);
			style = styles.get("SUB_TITLE2");
			cell.setCellValue(new HSSFRichTextString(create_time));
			if (style != null)
				cell.setCellStyle(style);
			
			rownum = 3;// 如果rownum = 1，则去掉创建人、创建时间等副标题；如果rownum = 0， 则把标题也去掉
			
			HSSFCellStyle headerstyle = styles.get("TABLE_HEADER");
			
			int colnum = 0;
			for (int i = 0; i < headerMetaData.getOriginColumns().size(); i++) {
				TableColumn tc = headerMetaData.getOriginColumns().get(i);
				if (i != 0) {
					colnum += headerMetaData.getOriginColumns().get(i - 1).getLength();
				}
				generateColumn(sheet, tc, headerMetaData.maxlevel, rownum, colnum, headerstyle);
			}
			rownum += headerMetaData.maxlevel;
			
			List<TableDataRow> dataRows = tableData.getRows();
			
			int index = 0;
			for (TableDataRow dataRow : dataRows) {
				row = sheet.createRow(rownum);
				
				List<TableDataCell> dataCells = dataRow.getCells();
				int size = headerMetaData.getColumns().size();
				index = -1;
				for (int i = 0; i < size; i++) {
					TableColumn tc = headerMetaData.getColumns().get(i);
					if (!tc.isVisible())
						continue;
					index++;
					
					createCell(row, tc, dataCells, i, index, styles);
				}
				rownum++;
			}
			// 设置前两列根据数据自动列宽
			for (int c = 0; c < headerMetaData.getColumns().size(); c++) {
				sheet.autoSizeColumn((short) c);
				String t = headerMetaData.getColumns().get(c).getDisplay();
				if(sheet.getColumnWidth(c)<t.length()*256*3)
					sheet.setColumnWidth(c, t.length()*256*3);
			}
			sheet.setGridsPrinted(true);
		}
		
		return wb;
	}
	
	/**
	 * 导出Excel(单工作表)
	 * @param title
	 * @param tableData
	 * @throws Exception
	 */
	public void exportToExcel(String title, TableData tableData)
			throws Exception {
		exportToExcel(title,tableData,0);
	}
	

	/**
	 * 导出Excel(单工作表)
	 * 
	 * @param title
	 *            文件名
	 * @param tableData
	 *            表格数据
	 * @param freezeColumnCount
	 * 			   冻结的列数
	 */
	public void exportToExcel(String title, TableData tableData, int freezeColumnCount)
			throws Exception {

		HSSFWorkbook wb = new HSSFWorkbook();// 创建新的Excel 工作簿
		
		HashMap<String, HSSFCellStyle> styles = initStyles(wb);// 根据模板文件，初始化表头样式
		
		wb = writeSheet(wb,title,styles,tableData,freezeColumnCount);//写入工作表

		String sFileName = title + ".xls";
		response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(sFileName, "UTF-8"))));
	 
		response.setHeader("Connection", "close");
		response.setHeader("Content-Type", "application/vnd.ms-excel");

		wb.write(response.getOutputStream());
	}
	
	/**
	 * 导出Excel(多工作表)
	 * 
	 * @param title
	 *            文件名
	 * @param creator
	 *            创建人
	 * @param tableDataLst
	 *            各工作格数据(注意：每个tableData要设置sheet名称，否则按默认呈现)
	 * @return void <style name="dataset"> case SYSROWNUM%2==0?#row0:#row1;
	 *         fontsize:9px; </style> <style name="row0"> import(parent);
	 *         bgcolor:#FFFFFF; </style> <style name="row1"> import(parent);
	 *         bgcolor:#CAEAFE; </style>
	 */
	public void exportToExcel(String title, String creator, List<TableData> tableDataLst)
		throws Exception {
		
		HSSFWorkbook wb = new HSSFWorkbook();// 创建新的Excel 工作簿
		HashMap<String, HSSFCellStyle> styles = initStyles(wb);// 初始化表头样式

		int i = 1;
		for(TableData tableData : tableDataLst){
			String sheetTitle = tableData.getSheetTitle();
			sheetTitle = sheetTitle==null||sheetTitle.equals("")?"sheet"+i:sheetTitle;
			wb = writeSheet(wb,tableData.getSheetTitle(),styles,tableData,0);//写入工作表
			i++;
		}
		
		String sFileName = title + ".xls";
		response.setHeader("Content-Disposition", "attachment;filename="
				.concat(String.valueOf(URLEncoder.encode(sFileName, "UTF-8"))));
		response.setHeader("Connection", "close");
		response.setHeader("Content-Type", "application/vnd.ms-excel");
		
		wb.write(response.getOutputStream());
	}
	
	

	/**
	 * 创建单元格（带隔行背景色）
	 * 
	 * @param
	 * @return void
	 */
	private void createCell(HSSFRow row, TableColumn tc,
                            List<TableDataCell> data, int i, int index,
                            HashMap<String, HSSFCellStyle> styles) {
		TableDataCell dc = data.get(i);
		HSSFCell cell = row.createCell(index);
		
		HSSFCellStyle sheetStyle = styles.get(SHEET_STYLE);//设置整体风格
		if (sheetStyle != null)
			cell.setCellStyle(sheetStyle);
		
		switch (tc.getColumnType()) {
		case TableColumn.COLUMN_TYPE_INTEGER:
			cell.setCellValue(dc.getIntValue());
			HSSFCellStyle style = styles.get("INT");
			if (row.getRowNum() % 2 != 0)
				style = styles.get("INT_C");
			if (style != null)
				cell.setCellStyle(style);
			break;
		case TableColumn.COLUMN_TYPE_FLOAT_2:
			cell.setCellValue(dc.getDoubleValue());
			style = styles.get("D2");
			if (row.getRowNum() % 2 != 0)
				style = styles.get("D2_C");
			if (style != null)
				cell.setCellStyle(style);
			break;
		case TableColumn.COLUMN_TYPE_FLOAT_3:
			cell.setCellValue(dc.getDoubleValue());
			style = styles.get("D3");
			if (row.getRowNum() % 2 != 0)
				style = styles.get("D3_C");
			if (style != null)
				cell.setCellStyle(style);
			break;
		case TableColumn.COLUMN_TYPE_RED_BG:
			cell.setCellValue(dc.getValue());
			style = styles.get("RED_BG");
			if (style != null)
				cell.setCellStyle(style);
			break;
		case TableColumn.COLUMN_TYPE_YELLOW_BG:
			cell.setCellValue(dc.getValue());
			style = styles.get("YELLOW_BG");
			if (style != null)
				cell.setCellStyle(style);
			break;
		case TableColumn.COLUMN_TYPE_GREEN_BG:
			cell.setCellValue(dc.getValue());
			style = styles.get("GREEN_BG");
			if (style != null)
				cell.setCellStyle(style);
			break;
		default:
			if (dc.getValue().equalsIgnoreCase("&nbsp;"))
				cell.setCellValue("");
			else
				cell.setCellValue(dc.getValue());
			style = styles.get("STRING");
			if (row.getRowNum() % 2 != 0)
				style = styles.get("STRING_C");
			if (style != null)
				cell.setCellStyle(style);
		}
		if(tc.isMarkBGColor()){
			String value = dc.getValue();
			if(value!=null&&value.indexOf("/")!=-1){
				String markbgcell[] = value.split("/");
				cell.setCellValue(markbgcell[0]);
				cell.setCellStyle(styles.get(markbgcell[1]));
				
			}
				
		}
	}

	/**
	 *
	 *初始化excel模板样式
	 * @param wb
	 * @return
	 */
	private HashMap<String, HSSFCellStyle> initStyles(HSSFWorkbook wb) {
		HashMap<String, HSSFCellStyle> ret = new HashMap<String, HSSFCellStyle>();
		try {
			
			HSSFCellStyle style = wb.createCellStyle();
			HSSFFont font = wb.createFont();
			font.setFontName("宋体");
			font.setFontHeightInPoints((short) 11);//设置字体大小 
			style.setFont(font);
			
			style.setBorderTop(HSSFCellStyle.BORDER_THIN);
		    style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		    style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		    style.setBorderRight(HSSFCellStyle.BORDER_THIN);
			style.setFillBackgroundColor(HSSFColor.BLACK.index);
			ret.put(SHEET_STYLE, style);
			
			HSSFCellStyle headerStyle = wb.createCellStyle();
			HSSFFont headerfont = wb.createFont();
			headerfont.setFontName("宋体");
			headerfont.setFontHeightInPoints((short) 11);//设置字体大小 
			headerfont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);//粗体显示
			headerStyle.setFont(headerfont);
			headerStyle.setWrapText(true);
			headerStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			headerStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			headerStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
			ret.put(SHEET_HEADER_STYLE, headerStyle);
			
			HSSFCellStyle style1 = wb.createCellStyle();
			style1.setFillForegroundColor(HSSFColor.ROSE.index);
			style1.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND); 
			ret.put(MarkBGColorCell.MarkBGColor.ROSE.toString(), style1);
			
			HSSFCellStyle style2 = wb.createCellStyle();
			style2.setFillForegroundColor(HSSFColor.LIGHT_GREEN.index);      
			style2.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND); 
			ret.put(MarkBGColorCell.MarkBGColor.LIGHT_GREEN.toString(), style2);
			
			HSSFCellStyle style3 = wb.createCellStyle();
			style3.setFillForegroundColor(HSSFColor.CORNFLOWER_BLUE.index);      
			style3.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND); 
			ret.put(MarkBGColorCell.MarkBGColor.CORNFLOWER_BLUE.toString(), style3);
			
			HSSFCellStyle style4 = wb.createCellStyle();
			style4.setFillForegroundColor(HSSFColor.WHITE.index);      
			style4.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND); 
			ret.put(MarkBGColorCell.MarkBGColor.WHITE.toString(), style4);
			
			HSSFCellStyle style5 = wb.createCellStyle();
			style5.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);      
			style5.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND); 
			ret.put(MarkBGColorCell.MarkBGColor.GRAY.toString(), style5);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * 
	 * @param
	 * @return void
	 */
	private void buildStyle(HSSFWorkbook wb, HSSFWorkbook src, HSSFSheet sheet,
                            int index, HashMap<String, HSSFCellStyle> ret, String key) {
		HSSFRow row = sheet.getRow(index);
		HSSFCell cell = row.getCell(1);
		HSSFCellStyle nstyle = wb.createCellStyle();
		ExcelUtils.copyCellStyle(wb, nstyle, src, cell.getCellStyle());
		ret.put(key, nstyle);
	}


}

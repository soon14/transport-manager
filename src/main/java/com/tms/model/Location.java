package com.tms.model;

import com.tms.controller.vo.request.CreateOrderLocationRequestVo;
import org.springframework.beans.BeanUtils;
import org.springframework.data.geo.Point;

import javax.persistence.*;

/**
 * Created by szj on 2017/12/5.
 */
@Entity
public class Location extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocationType locationType;
    private String name;
    private String phone;
    private Long provinceCode;
    private Long cityCode;
    private Long districtCode;
    private String address;
    @Column(columnDefinition = "Point")
    private Point geo;

    public Location() {
    }

    public Point getGeo() {
        return geo;
    }

    public void setGeo(Point geo) {
        this.geo = geo;
    }

    public Location(CreateOrderLocationRequestVo locationRequestVo) {
        BeanUtils.copyProperties(locationRequestVo, this);
    }

    public enum LocationType {
        CUSTOMER, DISTRIBUTION
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(Long provinceCode) {
        this.provinceCode = provinceCode;
    }

    public Long getCityCode() {
        return cityCode;
    }

    public void setCityCode(Long cityCode) {
        this.cityCode = cityCode;
    }

    public Long getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(Long districtCode) {
        this.districtCode = districtCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

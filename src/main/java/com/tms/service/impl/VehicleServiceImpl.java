package com.tms.service.impl;

import com.tms.controller.vo.request.CreateVehicleRequestVo;
import com.tms.controller.vo.request.QueryVehicleRequestVo;
import com.tms.controller.vo.response.TraceResponseVo;
import com.tms.controller.vo.response.VehicleResponseVo;
import com.tms.model.Vehicle;
import com.tms.repository.SysCodeRepository;
import com.tms.repository.TraceRepository;
import com.tms.repository.VehicleRepository;
import com.tms.service.VehicleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private SysCodeRepository sysCodeRepository;
    @Autowired
    private TraceRepository traceRepository;

    @Override
    public void createVehicle(CreateVehicleRequestVo createVehicleRequestVo) {
        Vehicle vehicle = new Vehicle(createVehicleRequestVo);
        vehicle.setVehicleType(sysCodeRepository.findOne(createVehicleRequestVo.getVehicleType()));
        vehicle.setVehicleSubType(sysCodeRepository.findOne(createVehicleRequestVo.getVehicleSubType()));
        vehicle.preInsert();
        vehicleRepository.save(vehicle);
    }

    @Override
    public void updateVehicle(CreateVehicleRequestVo createVehicleRequestVo) {
        Vehicle vehicle = new Vehicle(createVehicleRequestVo);
        vehicle.setVehicleType(sysCodeRepository.findOne(createVehicleRequestVo.getVehicleType()));
        vehicle.setVehicleSubType(sysCodeRepository.findOne(createVehicleRequestVo.getVehicleSubType()));
        vehicle.preUpdate();
        vehicleRepository.save(vehicle);
    }

    @Override
    public Page<VehicleResponseVo> queryVehicle(QueryVehicleRequestVo vehicleRequestVo, Pageable page) {
        Page domainPage = vehicleRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicate = new ArrayList<>();
            if (!StringUtils.isEmpty(vehicleRequestVo.getBrand())) {
                predicate.add(criteriaBuilder.equal(root.get("brand"), vehicleRequestVo.getBrand()));
            }
            if (vehicleRequestVo.getVehicleType() != null) {
                predicate.add(criteriaBuilder.equal(root.get("vehicleType"), vehicleRequestVo.getVehicleType()));
            }
            if (vehicleRequestVo.getVehicleSubType() != null) {
                predicate.add(criteriaBuilder.equal(root.get("vehicleSubType"), vehicleRequestVo.getVehicleSubType()));
            }
            if (!StringUtils.isEmpty(vehicleRequestVo.getPlateNumber())) {
                predicate.add(criteriaBuilder.equal(root.get("plateNumber"), vehicleRequestVo.getPlateNumber()));
            }
            if (!StringUtils.isEmpty(vehicleRequestVo.getDriveLicense())) {
                predicate.add(criteriaBuilder.equal(root.get("driveLicense"), vehicleRequestVo.getDriveLicense()));
            }
            if (!StringUtils.isEmpty(vehicleRequestVo.getOperatorLicense())) {
                predicate.add(criteriaBuilder.equal(root.get("operatorLicense"), vehicleRequestVo.getOperatorLicense()));
            }
            if (vehicleRequestVo.getState() != null) {
                predicate.add(criteriaBuilder.equal(root.get("state"), vehicleRequestVo.getState()));
            }
            if (!StringUtils.isEmpty(vehicleRequestVo.getCompany())) {
                predicate.add(criteriaBuilder.equal(root.get("company"), vehicleRequestVo.getCompany()));
            }
            if (!StringUtils.isEmpty(vehicleRequestVo.getOwner())) {
                predicate.add(criteriaBuilder.equal(root.get("owner"), vehicleRequestVo.getOwner()));
            }
            if (!StringUtils.isEmpty(vehicleRequestVo.getOwnerPhone())) {
                predicate.add(criteriaBuilder.equal(root.get("ownerPhone"), vehicleRequestVo.getOwnerPhone()));
            }
            if (vehicleRequestVo.getLoads() != null) {
                predicate.add(criteriaBuilder.equal(root.get("loads"), vehicleRequestVo.getLoads()));
            }
            if (vehicleRequestVo.getStartTime() != null) {
                predicate.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createTime").as(Date.class), vehicleRequestVo.getStartTime()));
            }
            if (vehicleRequestVo.getEndTime() != null) {
                predicate.add(criteriaBuilder.lessThan(root.get("createTime").as(Date.class), vehicleRequestVo.getEndTime()));
            }
            return criteriaQuery.where(predicate.toArray(new Predicate[predicate.size()])).getRestriction();
        }, page);

        Page voPage = domainPage.map((Converter<Vehicle, VehicleResponseVo>) vehicle -> {
            VehicleResponseVo vehicleResponseVo = new VehicleResponseVo();
            BeanUtils.copyProperties(vehicle, vehicleResponseVo);
            return vehicleResponseVo;
        });
        return voPage;
    }

    @Override
    public List<TraceResponseVo> queryTrace(Long vehicleId, Date start, Date end) {
        List traces = traceRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicate = new ArrayList<>();
            predicate.add(criteriaBuilder.equal(root.join("vehicle").get("id"), vehicleId));
            predicate.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createTime").as(Date.class), start));
            predicate.add(criteriaBuilder.lessThan(root.get("createTime").as(Date.class), start));
            return criteriaQuery.where(predicate.toArray(new Predicate[predicate.size()])).getRestriction();
        });
        List<TraceResponseVo> responseVos = new ArrayList<>();
        traces.forEach(trace -> {
            TraceResponseVo responseVo = new TraceResponseVo();
            BeanUtils.copyProperties(trace, responseVo);
            responseVos.add(responseVo);
        });
        return responseVos;
    }
}

package com.frame.download.service.impl;

import com.frame.api.dto.FeignTestDto;
import com.frame.api.service.FeignTestService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeignTestServiceImpl implements FeignTestService {

    @Override
    @RequestMapping("/feign")
    public FeignTestDto getFeign() {
        FeignTestDto dto = new FeignTestDto();
        dto.setName("我是测试 feign方式调用的接口001");
        dto.setAge(16);
        return dto;
    }
}

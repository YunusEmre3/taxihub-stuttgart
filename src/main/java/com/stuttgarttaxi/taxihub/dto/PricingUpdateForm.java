package com.stuttgarttaxi.taxihub.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PricingUpdateForm {

    private List<PricingRuleForm> rules = new ArrayList<>();
}

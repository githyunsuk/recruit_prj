package kr.co.sist.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import kr.co.sist.admin.dashboard.DashboardService;
import kr.co.sist.user.entity.InquiryEntity;

@Controller
@RequestMapping("/admin")
public class DashboardController {
	@Autowired
  private DashboardService dashboardService;
	 @GetMapping("/admin_dashboard")
   public String selectUserCnt(Model model) {
       List<Map<String, Object>> userCount = dashboardService.getUserCountByDate();
       model.addAttribute("userCount", userCount); // "userCountData"는 템플릿에서 사용할 이름
       

       List<Map<String, Object>> corpCount = dashboardService.getCorpCountByResume();
       model.addAttribute("corpCount",corpCount);
       
       List<Map<String, Object>> indCount = dashboardService.getCorpCountByIndustry();
       model.addAttribute("indCount",indCount);
       
       List<InquiryEntity> ask = dashboardService.getAsk();
       model.addAttribute("ask",ask);
       
       return "admin/admin_dashboard"; 
   }

}

package kr.co.sist.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.co.sist.admin.resume.ResumeService;

@Controller
@RequestMapping("/admin")
public class AdResumeController {
	 
		@Autowired
		 private ResumeService rs;
		 @GetMapping("/admin_resume")
			public String SelectResume(Model model) {
				//List<ResumeDTO> resume=rs.selectResume();
			 List<Map<String, Object>> resume=rs.selectResume2();
				model.addAttribute("resume",resume);
				return "admin/admin_resume";
			}
		 
		 @GetMapping("/admin_resume_search")
			public String SearchResume( @RequestParam(required = false) Integer resume_seq,
		      @RequestParam(required = false) String name,
		      Model model) {
				List<Map<String,Object>> resume=rs.searchResume(resume_seq, name);
				model.addAttribute("resume",resume);
				return "admin/admin_resume";
			}
}

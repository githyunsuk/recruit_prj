package kr.co.sist.admin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.co.sist.admin.corp.CorpService;
import kr.co.sist.corp.dto.CorpEntity;
import kr.co.sist.util.CipherUtil;

@Controller
@RequestMapping("/admin")
public class AdminCorpController {
	@Autowired
	private CorpService cs;
	@Autowired
	private CipherUtil cipherUtil;
	
	/*
	@GetMapping("/admin_corp")
	public String SearchCorp(Model model) {
		List<CorpEntity> corp=cs.searchAll();
		model.addAttribute("corp",corp);
		return "admin/admin_corp";
	}
	*/
/*
	@GetMapping("/admin_corp_search")
	public String SearchOneCorp(@RequestParam("content") String content,@RequestParam("type") String type, Model model) {
		List<CorpEntity> corp;
		switch(type) {
			case "num": corp=cs.searchNoMember(content); break;
			case "name": corp=cs.searchNameMember(content); break;
			default : corp=cs.searchAll();
		}
		model.addAttribute("corp",corp);
		return "admin/admin_corp";
	}
	*/
	@GetMapping("/admin_corp")
	public String SelectCorp2(Model model) {
		List<CorpEntity> corp=cs.selectCorp();
		List<String> corpInd=cs.selectCorpInd();
		model.addAttribute("corp",corp);
		model.addAttribute("corpInd",corpInd);
		return "admin/admin_corp";
	}
	
	@GetMapping("/admin_corp_search")
	public String SearchCorp2( @RequestParam(required = false) String corpNo,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) List<String> industry,
      Model model) {
		List<CorpEntity> corp=cs.searchCorp(corpNo, name, industry);
		List<String> corpInd=cs.selectCorpInd();
		model.addAttribute("corp",corp);
		model.addAttribute("corpInd",corpInd);
		return "admin/admin_corp";
	}
	
	@GetMapping("/admin_corp_detail")
	public String CorpDetail(@RequestParam String corpNo,
												Model model) {
		CorpEntity corp=cs.detailCorp(corpNo);
		model.addAttribute("corp", corp);
		return "admin/admin_corp_detail";
	}
	
	
}

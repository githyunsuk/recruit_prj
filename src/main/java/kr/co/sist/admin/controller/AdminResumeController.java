package kr.co.sist.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.co.sist.login.UserRepository;
import kr.co.sist.user.dto.LinkDTO;
import kr.co.sist.user.dto.ResumeResponseDTO;
import kr.co.sist.user.entity.ResumeEntity;
import kr.co.sist.user.entity.UserEntity;
import kr.co.sist.user.repository.ResumeRepository;
import kr.co.sist.user.service.ResumeService;
import kr.co.sist.util.CipherUtil;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminResumeController {

	private final ResumeService resumeService;
	private final ResumeRepository resumeRepository;
	private final UserRepository userRepository;
	private final CipherUtil cu;
	
	@GetMapping("/admin_resume/detail")
	public String resumeDetailPage(@RequestParam("resume_seq") int resumeSeq, Model model) {
		
		ResumeEntity resumeEntity = resumeRepository.findById(resumeSeq).orElse(null);
		
		UserEntity userEntity = userRepository.findById(resumeEntity.getEmail()).orElse(null);
		userEntity.setPhone(cu.decryptText(userEntity.getPhone()));
		userEntity.setBirth(userEntity.getBirth().substring(0, 4));
		model.addAttribute("user", userEntity);
		
		ResumeResponseDTO resumeData = resumeService.searchOneDetailResume(resumeSeq);

		model.addAttribute("resumeData", resumeData);
		model.addAttribute("resume", resumeData.getResume());
		model.addAttribute("links", resumeData.getLinks() != null ? resumeData.getLinks() : new LinkDTO());
		model.addAttribute("positions", resumeData.getPositions());
		model.addAttribute("skills", resumeData.getSkills());
		model.addAttribute("educations", resumeData.getEducations());
		model.addAttribute("careers", resumeData.getCareers());
		model.addAttribute("projects", resumeData.getProjects());
		model.addAttribute("additionals", resumeData.getAdditionals());
		model.addAttribute("introductions", resumeData.getIntroductions());
		
		return "/admin/admin_resume_detail";
	}
}

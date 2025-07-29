package kr.co.sist.user.controller;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.BaseFont;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.sist.jwt.CustomUser;
import kr.co.sist.login.UserRepository;
import kr.co.sist.pdf.ImgReplaceElementFactory;
import kr.co.sist.pdf.PdfService;
import kr.co.sist.user.dto.AttachmentDTO;
import kr.co.sist.user.dto.LinkDTO;
import kr.co.sist.user.dto.ResumeDTO;
import kr.co.sist.user.dto.ResumeRequestDTO;
import kr.co.sist.user.dto.ResumeResponseDTO;
import kr.co.sist.user.entity.UserEntity;
import kr.co.sist.user.service.AttachmentService;
import kr.co.sist.user.service.PositionCodeService;
import kr.co.sist.user.service.ResumeService;
import kr.co.sist.util.CipherUtil;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ResumeController {
	
	@Value("${upload.saveDir}")
	private String saveDir;

	private final ResumeService rServ;
	private final AttachmentService aServ;
	private final PositionCodeService pcs;

	private final UserRepository userRepos;

	private final ObjectMapper objMapper;
	private final CipherUtil cu;

	private final PdfService pdfService;
	private final ImgReplaceElementFactory imgReplaceElementFactory;

	// 이력서 관리 페이지로 이동
	@GetMapping("/user/resume/resume_management")
	public String resumeManagementPage(@AuthenticationPrincipal CustomUser userInfo, Model model) {

		// 토큰에서 유저 정보 빼오기
		UserEntity user = userRepos.findById(userInfo.getEmail()).orElse(null);
		if (user == null) {
			return "redirect:/accessDenied";
		}

		List<ResumeDTO> resumes = rServ.searchAllResumeByUser(userInfo.getEmail());
		List<AttachmentDTO> files = aServ.searchAllAttachment(userInfo.getEmail());

		model.addAttribute("user", user);
		model.addAttribute("resumes", resumes);
		model.addAttribute("files", files);

		return "user/resume/resume_management";
	}

	// 이력서 신규 작성
	@GetMapping("/user/resume/resume_create")
	public String resumeCreate(@AuthenticationPrincipal CustomUser userInfo, Model model) {

		// 토큰에서 유저 정보 빼오기
		UserEntity user = userRepos.findById(userInfo.getEmail()).orElse(null);

		int resumeSeq = rServ.addResume(user);

		return "redirect:/user/resume/resume_form/" + resumeSeq;
	}

	// 이력서 폼으로 넘어가기(신규 작성 후 or 기존 이력서 클릭)
	@GetMapping("/user/resume/resume_form/{resumeSeq}")
	public String resumeForm(@PathVariable int resumeSeq, Model model, @AuthenticationPrincipal CustomUser userInfo) {

		// 토큰에서 유저 정보 빼오기
		UserEntity user = userRepos.findById(userInfo.getEmail()).orElse(null);

		user.setPhone(cu.decryptText(user.getPhone()));
		user.setBirth(user.getBirth().substring(0, 4));
		model.addAttribute("user", user);

		model.addAttribute("positionList", pcs.searchAllPositionCode());
		ResumeResponseDTO resumeData = rServ.searchOneDetailResume(resumeSeq);
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

		return "user/resume/resume_form";
	}

	// 이력서 저장(수정)하기
	@PostMapping("/user/resume/resumeSubmit")
	@ResponseBody
	public Map<String, String> resumeSubmit(@RequestParam(required = false) MultipartFile profileImage,
			@RequestParam("resumeData") String resumeDataJson) {

		Map<String, String> result = new HashMap<>();
		try {
			ResumeRequestDTO rdd = objMapper.readValue(resumeDataJson, ResumeRequestDTO.class);
			rServ.modifyResume(rdd, profileImage, rdd.getBasicInfo().getResumeSeq());
			result.put("result", "success");

		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "error");
		}

		return result;
	}// resumeSubmit

	// 이력서 미리보기
	@PostMapping("/user/resume/resumePreview")
	@ResponseBody
	public Map<String, Object> resumePreview(@RequestParam(required = false) MultipartFile profileImage,
			@RequestParam("resumeData") String resumeDataJson) {
		Map<String, Object> result = new HashMap<>();
		try {
			ResumeRequestDTO rdd = objMapper.readValue(resumeDataJson, ResumeRequestDTO.class);
			rServ.modifyResume(rdd, profileImage, rdd.getBasicInfo().getResumeSeq());

			String previewUrl = "/user/resume/preview/" + rdd.getBasicInfo().getResumeSeq();

			result.put("result", "success");
			result.put("previewUrl", previewUrl);
		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "error");
		}
		return result;
	}

	// 이력서 미리보기 창 띄우기
	@GetMapping("/user/resume/preview/{resumeSeq}")
	public String resumePreviewPage(@PathVariable int resumeSeq, Model model,
			@AuthenticationPrincipal CustomUser userInfo) {

		// 토큰에서 유저 정보 빼오기
		UserEntity user = userRepos.findById(userInfo.getEmail()).orElse(null);
		user.setPhone(cu.decryptText(user.getPhone()));
		user.setBirth(user.getBirth().substring(0, 4));
		model.addAttribute("user", user);

		ResumeResponseDTO resumeData = rServ.searchOneDetailResume(resumeSeq);

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

		return "user/resume/resume_preview"; // 실제 보여줄 미리보기 페이지 뷰 이름
	}

	// 이력서 다운로드(pdf생성)
	@GetMapping("/user/resume/download/{resumeSeq}")
	public void downloadResume(@PathVariable int resumeSeq, @AuthenticationPrincipal CustomUser userInfo,
			HttpServletResponse response) {

		// 토큰에서 유저 정보 빼오기
		UserEntity user = userRepos.findById(userInfo.getEmail()).orElse(null);
		user.setPhone(cu.decryptText(user.getPhone()));
		user.setBirth(user.getBirth().substring(0, 4));

		ResumeResponseDTO resumeData = rServ.searchOneDetailResume(resumeSeq);

		// pdf로 넘길 데이터 Map 생성
		Map<String, Object> map = new HashMap<String, Object>();
		// 이미지 경로를 절대 경로로 설정
		String profileImg = resumeData.getResume().getImage();
		if (profileImg != null && !profileImg.isEmpty()) {
			// 이미지 경로를 PDF에서 인식할 수 있도록 설정
			String imagePath = "/images/profileImg/" + profileImg;
			
			map.put("profileImagePath", imagePath);
		}
		map.put("user", user);
		map.put("resumeData", resumeData);
		map.put("resume", resumeData.getResume());
		map.put("links", resumeData.getLinks() != null ? resumeData.getLinks() : new LinkDTO());
		map.put("positions", resumeData.getPositions());
		map.put("skills", resumeData.getSkills());
		map.put("educations", resumeData.getEducations());
		map.put("careers", resumeData.getCareers());
		map.put("projects", resumeData.getProjects());
		map.put("additionals", resumeData.getAdditionals());
		map.put("introductions", resumeData.getIntroductions());

		String pdfTitle = resumeData.getResume().getTitle();

		// pdf 생성
		try {
			String processHtml = pdfService.createPdf("user/resume/resume_pdf", map);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ITextRenderer renderer = new ITextRenderer();

			SharedContext sharedContext = renderer.getSharedContext();
			sharedContext.setPrint(true);
			sharedContext.setInteractive(false);
			sharedContext.setReplacedElementFactory(imgReplaceElementFactory);
			sharedContext.getTextRenderer().setSmoothingThreshold(0);

			renderer.getFontResolver().addFont(new ClassPathResource("/static/font/NanumBarunGothic.ttf").getURL().toString(),
					BaseFont.IDENTITY_H, BaseFont.EMBEDDED

			);
			renderer.setDocumentFromString(processHtml);

			renderer.layout();
			renderer.createPDF(outputStream);
			response.setContentType("application/pdf");
			String encodedFileName = URLEncoder.encode(pdfTitle, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
			response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName + ".pdf");

			response.getOutputStream().write(outputStream.toByteArray());
			response.getOutputStream().flush();

			renderer.finishPDF();
			outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// 이력서 삭제하기
	@PostMapping("/user/resume/resumeRemove/{resumeSeq}")
	@ResponseBody
	public Map<String, Object> resumeRemove(@PathVariable int resumeSeq) {
		
		Map<String, Object> result = new HashMap<>();

		rServ.removeResume(resumeSeq);

		result.put("result", "success");

		return result;
	}

}// class

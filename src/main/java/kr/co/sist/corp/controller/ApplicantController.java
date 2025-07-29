package kr.co.sist.corp.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.pdf.BaseFont;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.sist.corp.dto.ApplicantDTO;
import kr.co.sist.corp.dto.ApplicantSearchDTO;
import kr.co.sist.corp.dto.ApplicationAttachmentDTO;
import kr.co.sist.corp.dto.CorpEntity;
import kr.co.sist.corp.dto.JobPostingDTO;
import kr.co.sist.corp.service.ApplicantService;
import kr.co.sist.jwt.CustomUser;
import kr.co.sist.login.CorpRepository;
import kr.co.sist.login.UserRepository;
import kr.co.sist.pdf.ImgReplaceElementFactory;
import kr.co.sist.pdf.PdfService;
import kr.co.sist.user.dto.LinkDTO;
import kr.co.sist.user.dto.ResumeResponseDTO;
import kr.co.sist.user.entity.AttachmentEntity;
import kr.co.sist.user.entity.ResumeEntity;
import kr.co.sist.user.entity.UserEntity;
import kr.co.sist.user.service.AttachmentService;
import kr.co.sist.user.service.MessageService;
import kr.co.sist.user.service.ResumeService;
import kr.co.sist.util.CipherUtil;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ApplicantController {

	@Value("${upload.saveDir}")
	private String saveDir;

	private final CorpRepository corpRepos;
	private final UserRepository userRepos;

	private final ApplicantService applicantServ;
	private final ResumeService resumeServ;
	private final MessageService messageServ;
	private final PdfService pdfService;
	private final AttachmentService attachmentServ;

	private final ImgReplaceElementFactory imgReplaceElementFactory;

	private final CipherUtil cu;

	// 처음 지원자 관리 접근시 해당 회사의 모든 지원자 가져오기(직접 페이징 기능 구현)
	@GetMapping("/corp/applicant")
	public String applicant(@AuthenticationPrincipal CustomUser userInfo, @ModelAttribute ApplicantSearchDTO searchDTO,
			Model model) {

		// 기업 회원이 아니면 접근금지
		boolean hasCorpAuth = userInfo.getAuthorities().stream().anyMatch(auth -> "ROLE_CORP".equals(auth.getAuthority()));
		if (!hasCorpAuth) {
			return "redirect:/accessDenied";
		}

		CorpEntity corp = corpRepos.findById(userInfo.getCorpNo()).orElse(null);

		// 모든 지원자 수
		int totalCnt = applicantServ.searchAllApplicantCnt(corp.getCorpNo());

		// 검색 조건 및 페이징 DTO
		searchDTO.setTotalCnt(totalCnt);
		searchDTO.setCorpNo(corp.getCorpNo());

		// 검색된 지원자 수
		int searchCnt = applicantServ.searchApplicantCnt(searchDTO);
		searchDTO.setSearchCnt(searchCnt);

		// 지원자 목록
		List<ApplicantDTO> applicants = applicantServ.searchApplicant(searchDTO);

		model.addAttribute("applicants", applicants);
		model.addAttribute("search", searchDTO);

		return "corp/applicant/applicant";
	}// applicant

	// 공고 유형 선택에 따른 결과값 보내기(진행중, 마감된 공고)
	@PostMapping("/corp/applicant")
	@ResponseBody
	public Map<String, Object> getPostingTitle(@RequestParam(required = false, defaultValue = "") String status,
			@AuthenticationPrincipal CustomUser userInfo) {

		CorpEntity corp = corpRepos.findById(userInfo.getCorpNo()).orElse(null);

		Map<String, Object> result = new HashMap<String, Object>();
		List<JobPostingDTO> postings = new ArrayList<>();
		String defaultOption = "전체 공고";

		switch (status) {
		case "":
			postings = applicantServ.searchPostingAll(corp.getCorpNo());
			defaultOption = "전체 공고";
			break;
		case "progress":
			postings = applicantServ.searchPostingProgress(corp.getCorpNo());
			defaultOption = "전체 진행중 공고";
			break;
		case "closed":
			postings = applicantServ.searchPostingClosed(corp.getCorpNo());
			defaultOption = "전체 마감된 공고";
			break;
		}

		result.put("defaultOption", defaultOption);
		result.put("postings", postings);

		return result;
	}// getPostingTitle

	// 지원자 조건 검색
	@PostMapping("/corp/applicant/search")
	@ResponseBody
	public Map<String, Object> applicantSearch(@RequestBody ApplicantSearchDTO searchDTO,
			@AuthenticationPrincipal CustomUser userInfo) {
		Map<String, Object> result = new HashMap<String, Object>();

		CorpEntity corp = corpRepos.findById(userInfo.getCorpNo()).orElse(null);
		searchDTO.setCorpNo(corp.getCorpNo());

		List<ApplicantDTO> applicants = applicantServ.searchApplicant(searchDTO);
		result.put("applicants", applicants);

		return result;
	}// applicantSearch

	// 지원자 이력서 페이지로 넘어가기
	@GetMapping("/corp/applicant/resume/{resumeSeq}/{jobPostingSeq}/{jobApplicationSeq}")
	public String applicantResume(@PathVariable int resumeSeq, @PathVariable int jobPostingSeq,
			@PathVariable int jobApplicationSeq, Model model, @AuthenticationPrincipal CustomUser corpInfo) {

		CorpEntity corpEntity = corpRepos.findById(corpInfo.getCorpNo()).orElse(null);

		// 응답용 이력서 객체
		ResumeResponseDTO resumeData = resumeServ.searchOneDetailResume(resumeSeq);

		// 사용자 가져오기
		UserEntity userEntity = userRepos.findById(resumeData.getResume().getEmail()).orElse(null);

		// 지원서 열람 처리
		int cnt = applicantServ.modifyResumeReadStatus(resumeSeq, jobPostingSeq);

		// 지원서 첫 열람시 사용자에게 메일 보내기
		if (cnt > 0) {
			messageServ.addResumeReadNotification(userEntity, corpEntity, jobPostingSeq);
		}

		// 사용자 가공해서 바인딩
		userEntity.setPhone(cu.decryptText(userEntity.getPhone()));
		userEntity.setBirth(userEntity.getBirth().substring(0, 4));
		model.addAttribute("resumeUser", userEntity);

		// 해당 지원서 스크랩 여부 가져오기
		boolean isScraped = applicantServ.isScraped((long) resumeSeq, corpEntity.getCorpNo());

		// 해당 지원서의 첨부파일 가져오기
		List<ApplicationAttachmentDTO> attachments = applicantServ.searchApplicantAttachment(jobApplicationSeq);

		// 이력서 정보 바인딩
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
		model.addAttribute("isScraped", isScraped);
		model.addAttribute("jobPostingSeq", jobPostingSeq);
		model.addAttribute("attachments", attachments);

		return "corp/applicant/applicant_resume";
	}

	// 북마크 추가
	@PostMapping("/corp/bookmark/add/{resumeSeq}")
	@ResponseBody
	public String addBookmark(@PathVariable int resumeSeq, @AuthenticationPrincipal CustomUser corpInfo) {

		CorpEntity corpEntity = corpRepos.findById(corpInfo.getCorpNo()).orElse(null);

		applicantServ.addBoomark((long) resumeSeq, corpEntity.getCorpNo());

		return "success";
	}

	// 북마크 제거
	@PostMapping("/corp/bookmark/remove/{resumeSeq}")
	@ResponseBody
	public String removeBookmark(@PathVariable int resumeSeq, @AuthenticationPrincipal CustomUser corpInfo) {

		CorpEntity corpEntity = corpRepos.findById(corpInfo.getCorpNo()).orElse(null);

		applicantServ.removeBookmark(resumeSeq, corpEntity.getCorpNo());

		return "success";
	}

	// 지원 상태 변경시 메시지 보내기
	@PostMapping("/corp/applicant/message")
	public String sendMessageToApplicant(int passStage, String messageTitle, String messageContent, int resumeSeq,
			String email, int jobPostingSeq, @AuthenticationPrincipal CustomUser corpInfo) {

		CorpEntity corpEntity = corpRepos.findById(corpInfo.getCorpNo()).orElse(null);

		applicantServ.modifyPassStage(resumeSeq, jobPostingSeq, passStage); // 합격 상태 변경하기
		messageServ.addMessage(email, corpEntity.getCorpNo(), messageTitle, messageContent);

		return "redirect:/corp/applicant";
	}

	// 첨부파일 다운로드
	@GetMapping("/corp/attachment/download/{attachmentSeq}")
	public ResponseEntity<Resource> downloadAttachment(@PathVariable int attachmentSeq) {

		try {
			// DB에서 파일 정보 가져오기
			AttachmentEntity attachment = attachmentServ.searchOneAttachment(attachmentSeq);
			String fileName = attachment.getFileName();
			String originalName = attachment.getOriginalName();

			// 상대 경로 위해 사용
//			String projectPath = new File("").getAbsolutePath(); // 현재 프로젝트 루트
//			String resourcePath = projectPath + "/src/main/resources/static/attachment";
			// --------------------

			// 배포시 사용
			String resourcePath = saveDir + "/attachment";

			File file = new File(resourcePath, fileName);

			if (!file.exists()) {
				return ResponseEntity.notFound().build();
			}

			// 파일을 리소스로 감싸기
			FileSystemResource resource = new FileSystemResource(file);

			// 다운로드시 파일명이 깨지지 않도록 인코딩
			String encodedName = URLEncoder.encode(originalName, "UTF-8").replace("+", "%20");
			String contentDisposition = "attachment; filename*=UTF-8''" + encodedName;

			// 파일 타입 설정 (못 찾으면 기본값 설정)
			String contentType = Files.probeContentType(file.toPath());
			if (contentType == null) {
				contentType = "application/octet-stream"; // 기본 타입
			}

			// 파일과 헤더를 담아 응답
			return ResponseEntity.ok().header("Content-Disposition", contentDisposition).header("Content-Type", contentType)
					.contentLength(file.length()).body(resource);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().build();
		}
	}

	// 지원자 이력서 PDF 다운로드
	@GetMapping("/corp/applicant/download/{resumeSeq}")
	public void downloadResume(@PathVariable int resumeSeq, HttpServletResponse response) {

		ResumeResponseDTO resumeData = resumeServ.searchOneDetailResume(resumeSeq);

		ResumeEntity resume = resumeData.getResume();

		// 이력서에서 유저 정보 빼오기
		UserEntity user = userRepos.findById(resume.getEmail()).orElse(null);
		user.setPhone(cu.decryptText(user.getPhone()));
		user.setBirth(user.getBirth().substring(0, 4));

		// pdf로 넘길 데이터 Map 생성
		Map<String, Object> map = new HashMap<String, Object>();
		// 이미지 경로를 절대 경로로 설정
		String profileImg = resumeData.getResume().getImage();
		if (profileImg != null && !profileImg.isEmpty()) {
			// 이미지 경로를 PDF에서 인식할 수 있도록 설정
//			String imagePath = "/images/profileImg/" + profileImg;
			// 배포시 사용
			String imagePath = saveDir + "/images/profileImg/" + profileImg;
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

	}// downloadResume

	@GetMapping("/corp/applicant/download/all")
	public void downloadMultipleResumes(HttpServletResponse response, @ModelAttribute ApplicantSearchDTO searchDTO,
			@AuthenticationPrincipal CustomUser corpInfo) {
		try {
			CorpEntity corp = corpRepos.findById(corpInfo.getCorpNo()).orElse(null);
			searchDTO.setCorpNo(corp.getCorpNo());

			List<ApplicantDTO> applicants = applicantServ.searchApplicantForExcel(searchDTO);
			List<Integer> resumeSeqs = new ArrayList<>();
			for (ApplicantDTO applicant : applicants) {
				resumeSeqs.add(applicant.getResumeSeq());
			}

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ITextRenderer renderer = new ITextRenderer();

			// SharedContext 설정
			SharedContext sharedContext = renderer.getSharedContext();
			sharedContext.setPrint(true);
			sharedContext.setInteractive(false);
			sharedContext.setReplacedElementFactory(imgReplaceElementFactory);
			sharedContext.getTextRenderer().setSmoothingThreshold(0);

			// 폰트 추가
			renderer.getFontResolver().addFont(new ClassPathResource("/static/font/NanumBarunGothic.ttf").getURL().toString(),
					BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

			boolean isFirst = true;

			for (int resumeSeq : resumeSeqs) {
				ResumeResponseDTO resumeData = resumeServ.searchOneDetailResume(resumeSeq);
				ResumeEntity resume = resumeData.getResume();

				UserEntity user = userRepos.findById(resume.getEmail()).orElse(null);
				user.setPhone(cu.decryptText(user.getPhone()));
				user.setBirth(user.getBirth().substring(0, 4));

				Map<String, Object> map = new HashMap<>();
				String profileImg = resume.getImage();
				if (profileImg != null && !profileImg.isEmpty()) {
					String imagePath = "/images/profileImg/" + profileImg;
					map.put("profileImagePath", imagePath);
				}

				map.put("user", user);
				map.put("resumeData", resumeData);
				map.put("resume", resume);
				map.put("links", resumeData.getLinks() != null ? resumeData.getLinks() : new LinkDTO());
				map.put("positions", resumeData.getPositions());
				map.put("skills", resumeData.getSkills());
				map.put("educations", resumeData.getEducations());
				map.put("careers", resumeData.getCareers());
				map.put("projects", resumeData.getProjects());
				map.put("additionals", resumeData.getAdditionals());
				map.put("introductions", resumeData.getIntroductions());

				String processHtml = pdfService.createPdf("user/resume/resume_pdf", map);

				if (isFirst) {
					// 첫 문서
					renderer.setDocumentFromString(processHtml);
					renderer.layout();
					renderer.createPDF(outputStream, false); // false: 이어서 쓰기
					isFirst = false;
				} else {
					// 두 번째 이후 문서
					renderer.setDocumentFromString(processHtml);
					renderer.layout();
					renderer.writeNextDocument();
				}
			}

			renderer.finishPDF();

			String fileName = URLEncoder.encode("지원자_이력서_모음", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".pdf");

			response.getOutputStream().write(outputStream.toByteArray());
			response.getOutputStream().flush();
			outputStream.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 엑셀 파일 다운로드
	@GetMapping("/corp/applicant/excel")
	public void createExcel(HttpServletResponse response, @ModelAttribute ApplicantSearchDTO searchDTO,
			@AuthenticationPrincipal CustomUser corpInfo) {
		CorpEntity corp = corpRepos.findById(corpInfo.getCorpNo()).orElse(null);
		searchDTO.setCorpNo(corp.getCorpNo());

		try {

			Workbook workbook = new SXSSFWorkbook();
			Sheet sheet = workbook.createSheet();

			List<ApplicantDTO> applicants = applicantServ.searchApplicantForExcel(searchDTO);

			int rowIdx = 0;
			Row headerRow = sheet.createRow(rowIdx++);

			// 스타일 생성 (Bold + 정렬)
			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);
			headerStyle.setAlignment(HorizontalAlignment.CENTER);

			String[] headers = { "지원자명", "경력여부", "지원공고", "지원상태", "이력서", "지원날짜", "합격상태" };

			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);

				// 열 너비 설정
				switch (headers[i]) {
				case "지원공고":
					sheet.setColumnWidth(i, 30 * 256); // 30자 폭
					break;
				case "지원날짜":
					sheet.setColumnWidth(i, 20 * 256); // 20자 폭
					break;
				default:
					sheet.setColumnWidth(i, 10 * 256); // 기본 10자 폭
					break;
				}
			}

			for (ApplicantDTO applicant : applicants) {
				Row bodyRow = sheet.createRow(rowIdx++);

				Cell bodyCelA = bodyRow.createCell(0);
				bodyCelA.setCellValue(applicant.getName());
				Cell bodyCelB = bodyRow.createCell(1);
				bodyCelB.setCellValue(applicant.getCareerType());
				Cell bodyCelC = bodyRow.createCell(2);
				bodyCelC.setCellValue(applicant.getPostingTitle());
				Cell bodyCelD = bodyRow.createCell(3);
				bodyCelD.setCellValue(applicant.getStatusName());
				Cell bodyCelE = bodyRow.createCell(4);
				bodyCelE.setCellValue(applicant.getTitle());
				Cell bodyCelF = bodyRow.createCell(5);
				bodyCelF.setCellValue(applicant.getApplicationDate());
				Cell bodyCelG = bodyRow.createCell(6);
				bodyCelG.setCellValue(applicant.getStageName());

			}

			response.setContentType("application/vnd.ms-excel");
			String fileName = URLEncoder.encode("지원자 정보", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
			response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
			workbook.write(response.getOutputStream());
			workbook.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

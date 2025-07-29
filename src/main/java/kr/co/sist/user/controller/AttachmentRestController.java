package kr.co.sist.user.controller;

import java.io.File;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.co.sist.jwt.CustomUser;
import kr.co.sist.jwt.JWTUtil;
import kr.co.sist.login.UserRepository;
import kr.co.sist.user.entity.AttachmentEntity;
import kr.co.sist.user.entity.UserEntity;
import kr.co.sist.user.service.AttachmentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AttachmentRestController {

	@Value("${upload.saveDir}")
	private String saveDir;
	
	private final JWTUtil jwtUtil;
	private final AttachmentService attachmentServ;
	private final UserRepository userRepos;

	// 첨부파일 추가
	@PostMapping("/user/attachment")
	public Map<String, Object> uploadFile(@RequestParam MultipartFile file, @AuthenticationPrincipal CustomUser userInfo) {

		Map<String, Object> result = new HashMap<>();

		UserEntity user = userRepos.findById(userInfo.getEmail()).orElse(null);
		
		AttachmentEntity attachment = attachmentServ.addAttachment(file, user);

		if (attachment != null) {
			result.put("result", "success");
			result.put("file", attachment);
		} else {
			result.put("result", "fail");
		}

		return result;
	}// uploadFile

	// 첨부파일 삭제
	@DeleteMapping("/user/attachment/{attachmentSeq}")
	public Map<String, Object> fileRemove(@PathVariable int attachmentSeq) {

		Map<String, Object> result = new HashMap<>();

		attachmentServ.removeAttachment(attachmentSeq);

		result.put("result", "success");

		return result;
	}

	/**
	 * 첨부파일 다운로드(이거 꼭 제대로 공부해보자!)
	 * @param attachmentSeq
	 * @return
	 */
	@GetMapping("/user/attachment/download/{attachmentSeq}")
	public ResponseEntity<Resource> downloadFile(@PathVariable int attachmentSeq) {
	    try {
	        // DB에서 파일 정보 가져오기
	        AttachmentEntity attachment = attachmentServ.searchOneAttachment(attachmentSeq);
	        String fileName = attachment.getFileName();
	        String originalName = attachment.getOriginalName();
	        
	        //상대 경로 위해 사용
	    		String projectPath = new File("").getAbsolutePath(); // 현재 프로젝트 루트
	    		String resourcePath = projectPath + "/src/main/resources/static/attachment";
	    		//--------------------
	        
	    		
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
	        return ResponseEntity.ok()
	                .header("Content-Disposition", contentDisposition)
	                .header("Content-Type", contentType)
	                .contentLength(file.length())
	                .body(resource);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.badRequest().build();
	    }
	}//downloadFile
	
}

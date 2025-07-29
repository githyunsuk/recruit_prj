package kr.co.sist.user.service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import kr.co.sist.user.dto.AttachmentDTO;
import kr.co.sist.user.entity.AttachmentEntity;
import kr.co.sist.user.entity.UserEntity;
import kr.co.sist.user.mapper.ResumeMapper;
import kr.co.sist.user.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttachmentService {

	@Value("${upload.saveDir}")
	private String saveDir;

	private final AttachmentRepository attachmentRepos;
	private final ResumeMapper resumeMapper;

	/**
	 * 첨부파일 업로드
	 * 
	 * @param file
	 * @param user
	 * @return
	 */
	public AttachmentEntity addAttachment(MultipartFile file, UserEntity user) {

		LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

		// 1.유저 정보 추출
		String email = user.getEmail();

		// 2. 파일 정보 추출
		String originalName = file.getOriginalFilename();
		String extension = originalName.substring(originalName.lastIndexOf('.') + 1);
		String savedFileName = "attachment_" + UUID.randomUUID().toString() + "." + extension;
		long fileSize = file.getSize();

		// 3. 중복되는 파일 있는지 체크
		List<AttachmentDTO> lists = searchAllAttachment(email);
		for(AttachmentDTO dto : lists) {
			if (originalName.equals(dto.getOriginalName())) return null;
		
		}
		
		// 4. 첨부파일 엔티티 생성 및 저장
		AttachmentEntity attachment = new AttachmentEntity();
		attachment.setEmail(email);
		attachment.setFileName(savedFileName);
		attachment.setOriginalName(originalName);
		attachment.setFileSize(fileSize);
		attachment.setFileType(extension);
		attachment.setCreatedAt(now.toString());

		//상대 경로 위해 사용
		String projectPath = new File("").getAbsolutePath(); // 현재 프로젝트 루트
		String resourcePath = projectPath + "/src/main/resources/static/attachment";
		//--------------------
		
		
		attachmentRepos.save(attachment);
		attachment.setCreatedAt(attachment.getCreatedAt().substring(0, 10));

		// 4. 파일 저장 경로 설정 및 실제 파일 저장
		//File uploadDir = new File(fileDir);
		File uploadDir = new File(resourcePath);
		if (!uploadDir.exists())
			uploadDir.mkdir(); // 하위 폴더까지 생성

		File destination = new File(uploadDir, savedFileName);
		try {
			file.transferTo(destination);
			return attachment;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}// searchAllAttachment

	/**
	 * 첨부파일seq로 첨부파일 찾기
	 * 
	 * @param attachmentSeq
	 * @return
	 */
	public AttachmentEntity searchOneAttachment(int attachmentSeq) {

		return attachmentRepos.findById(attachmentSeq).orElse(null);
	}

	/**
	 * 해당 유저의 모든 첨부파일 가져오기
	 * 
	 * @param email
	 * @return
	 */
	public List<AttachmentDTO> searchAllAttachment(String email) {

		List<AttachmentDTO> attachments = resumeMapper.selectAllAttachment(email);
		for (AttachmentDTO attachment : attachments) {
			attachment.setCreatedAt(attachment.getCreatedAt().substring(0, 10)); // 날짜 포맷팅
		}

		return attachments;

	}

	/**
	 * 해당 첨부파일 삭제하기
	 * 
	 * @param attachmentSeq
	 */
	public void removeAttachment(int attachmentSeq) {

		attachmentRepos.deleteById(attachmentSeq);
	}// removeAttachment

}// class

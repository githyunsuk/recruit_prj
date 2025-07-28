package kr.co.sist.user.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import kr.co.sist.user.dto.ResumeDTO;
import kr.co.sist.user.dto.ResumeRequestDTO;
import kr.co.sist.user.dto.ResumeResponseDTO;
import kr.co.sist.user.entity.AdditionalInfoEntity;
import kr.co.sist.user.entity.CareerEntity;
import kr.co.sist.user.entity.EducationHistoryEntity;
import kr.co.sist.user.entity.LinkEntity;
import kr.co.sist.user.entity.ResumeEntity;
import kr.co.sist.user.entity.ResumePositionCodeEntity;
import kr.co.sist.user.entity.ResumeTechStackEntity;
import kr.co.sist.user.entity.SelfIntroductionEntity;
import kr.co.sist.user.entity.UserEntity;
import kr.co.sist.user.mapper.ResumeMapper;
import kr.co.sist.user.repository.AdditionalInfoRepository;
import kr.co.sist.user.repository.CareerRepository;
import kr.co.sist.user.repository.EducationHistoryRepository;
import kr.co.sist.user.repository.LinkRepository;
import kr.co.sist.user.repository.ResumePositionCodeRepository;
import kr.co.sist.user.repository.ResumeRepository;
import kr.co.sist.user.repository.ResumeTechStackRepository;
import kr.co.sist.user.repository.SelfIntroductionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeService {

	@Value("${upload.saveDir}")
	private String saveDir;

	// Mapper 생성자주입
	private final ResumeMapper rMapper;

	// Repository 생성자주입
	private final ResumeRepository rRepos;
	private final ResumePositionCodeRepository rpcRepos;
	private final ResumeTechStackRepository rtsRepos;
	private final LinkRepository lRepos;
	private final EducationHistoryRepository ehRepos;
	private final CareerRepository cRepos;
	private final AdditionalInfoRepository aiRepos;
	private final SelfIntroductionRepository siRepos;

	// Service 생성자주입
	private final ProjectService pServ;


	/**
	 * 특정 유저의 모든 이력서 가져오기
	 * 
	 * @param email
	 * @return
	 */
	public List<ResumeDTO> searchAllResumeByUser(String email) {

		List<ResumeDTO> resumeList = rMapper.selectAllResumeByUser(email);
		for (ResumeDTO resume : resumeList) {
			resume.setCreatedAt(resume.getCreatedAt().substring(0, 10)); // 날짜 포맷팅
		}

		return resumeList;
	}

	/**
	 * 이력서 생성
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 */
	public int addResume(UserEntity user) throws IllegalArgumentException {
		LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		ResumeEntity re = new ResumeEntity();

		re.setEmail(user.getEmail());
		re.setCreatedAt(now.toString());

		// 날짜 형식 "yyMMdd" 만들기
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyMMdd");
		String date = now.format(dtf);

		re.setTitle(user.getName() + "_" + date);
		re.setIsPublic("Y");
		re.setCareerType("N");

		int resumeSeq = rRepos.save(re).getResumeSeq();

		return resumeSeq;
	}// addResume

	/**
	 * 해당 이력서 삭제
	 * 
	 * @param resumeSeq
	 */
	@Transactional
	public void removeResume(int resumeSeq) {
		 try {
       System.out.println("삭제 시작--------");
       rMapper.deleteResume(resumeSeq);
       System.out.println("삭제 완료");
   } catch (Exception e) {
       e.printStackTrace();
   }
	}

	/**
	 * 이력서 수정
	 * 
	 * @param rdd
	 * @return
	 * @throws IllegalArgumentException
	 */
	@Transactional
	public boolean modifyResume(ResumeRequestDTO rdd, MultipartFile profileImage, int resumeSeq)
			throws IllegalArgumentException {
		LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS); // 현재 날짜 초 까지만 저장

		// 이력서 레코드 수정
		ResumeEntity re = searchOneResume(resumeSeq); // 기존 이력서 레코드 가져오기
		ResumeEntity rEntity = rdd.getBasicInfo();

		// 이미지 업로드
		if (profileImage != null && !profileImage.isEmpty()) {
			try {
				uploadImg(profileImage, re);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		re.setIsPublic(Boolean.parseBoolean(rEntity.getIsPublic()) ? "Y" : "N"); // 이력서 공개 여부 처리
		re.setCareerType((rdd.getCareers() != null && !rdd.getCareers().isEmpty()) ? "E" : "N"); // 경력 여부 처리
		re.setIntroduction(rEntity.getIntroduction());
		re.setTitle(rEntity.getTitle());
		re.setUpdatedAt(now.toString());

		rRepos.save(re); // 이력서 레코드 수정

		// 저장하기 전 기존 데이터들 일괄 삭제
		removeForUpdateResume(resumeSeq);

		// 이력서-포지션코드 레코드 생성 및 수정
		List<ResumePositionCodeEntity> positions = rdd.getPositions();
		for (ResumePositionCodeEntity position : positions) {
			ResumePositionCodeEntity rpce = new ResumePositionCodeEntity();
			rpce.setResumeSeq(resumeSeq);
			rpce.setPositionSeq(position.getPositionSeq());
			rpcRepos.save(rpce);
		}

		// 이력서-기술스택 레코드 생성 및 수정
		List<ResumeTechStackEntity> skills = rdd.getSkills();
		for (ResumeTechStackEntity skill : skills) {
			ResumeTechStackEntity rtse = new ResumeTechStackEntity();
			rtse.setResumeSeq(resumeSeq);
			rtse.setTechStackSeq(skill.getTechStackSeq());
			rtsRepos.save(rtse);
		}

		// 링크 레코드 생성 및 수정
		LinkEntity le = rdd.getLinks();
		le.setResumeSeq(resumeSeq);
		lRepos.save(le);

		// 학력 레코드 생성 및 수정
		List<EducationHistoryEntity> educations = rdd.getEducations();
		educations.sort(Comparator.comparing(EducationHistoryEntity::getAdmissionDate)); // 입학날짜를 기준으로 정렬
		int indexNum = 1; // 순번용 인덱스
		for (EducationHistoryEntity education : educations) {
			education.setResumeSeq(resumeSeq);
			education.setIndexNum(indexNum++);
			ehRepos.save(education);
		}

		// 경력 레코드 생성 및 수정
		List<CareerEntity> careers = rdd.getCareers();
		careers.sort(Comparator.comparing(CareerEntity::getStartDate)); // 입사날짜를 기준으로 정렬
		indexNum = 1;
		for (CareerEntity career : careers) {
			career.setResumeSeq(resumeSeq);
			career.setIndexNum(indexNum++);
			cRepos.save(career);
		}

		// 프로젝트 및 프로젝스-기술스택 레코드 생성 및 수정
		pServ.addProjectAndSkills(resumeSeq, rdd.getProjects(), rdd.getProjectSkills());

		// 기타사항 레코드 생성 및 수정
		List<AdditionalInfoEntity> etcs = rdd.getEtc();
		for (AdditionalInfoEntity etc : etcs) {
			etc.setResumeSeq(resumeSeq);
			aiRepos.save(etc);
		}

		// 자기소개서 레코드 생성 및 수정
		List<SelfIntroductionEntity> intros = rdd.getIntroductions();
		for (SelfIntroductionEntity intro : intros) {
			intro.setResumeSeq(resumeSeq);
			siRepos.save(intro);
		}

		return true;
	}// addModifyResume

	/**
	 * seq로 이력서 하나 가져오기
	 * 
	 * @param resumeSeq
	 * @return
	 */
	public ResumeEntity searchOneResume(int resumeSeq) {

		ResumeEntity re = rRepos.findById(resumeSeq).orElse(null);

		return re;
	}// searchOneResume

	/**
	 * seq로 해당 이력서와 연결된 테이블들의 데이터 모두 가져오기
	 * 
	 * @param resumeSeq
	 * @return
	 */
	public ResumeResponseDTO searchOneDetailResume(int resumeSeq) {
		ResumeResponseDTO rrDTO = new ResumeResponseDTO();

		rrDTO.setResume(searchOneResume(resumeSeq));
		rrDTO.setPositions(rMapper.selectPositionByResume(resumeSeq));
		rrDTO.setLinks(rMapper.selectLinkByResume(resumeSeq));
		rrDTO.setSkills(rMapper.selectStackByResume(resumeSeq));
		rrDTO.setEducations(rMapper.selectEduByResume(resumeSeq));
		rrDTO.setCareers(rMapper.selectCareerByResume(resumeSeq));

		rrDTO.setProjects(pServ.searchProjectByResume(resumeSeq));

		rrDTO.setAdditionals(rMapper.selectEtcByResume(resumeSeq));
		rrDTO.setIntroductions(rMapper.selectIntroByResume(resumeSeq));

		return rrDTO;

	}// searchOneDetailResume

	/**
	 * 이력서 수정하기 위해 이력서 요소들 일괄 삭제
	 * 
	 * @param resumeSeq
	 */
	@Transactional
	public void removeForUpdateResume(int resumeSeq) {
		rMapper.deleteCareerByResume(resumeSeq);
		rMapper.deleteEducationByResume(resumeSeq);
		rMapper.deleteIntroByResume(resumeSeq);
		rMapper.deleteLinkByResume(resumeSeq);
		rMapper.deletePositionByResume(resumeSeq);
		rMapper.deleteStackByResume(resumeSeq);
		rMapper.deleteAdditionalByResume(resumeSeq);
		rMapper.deleteProjectByResume(resumeSeq);
	}

	/**
	 * 프로필 이미지 업로드
	 * 
	 * @param mf
	 * @param re
	 * @throws IOException
	 */
	public void uploadImg(MultipartFile mf, ResumeEntity re) throws IOException {

		
		// 상대 경로 위해 사용
//		String projectPath = new File("").getAbsolutePath(); // 현재 프로젝트 루트
//		String resourcePath = projectPath + "/src/main/resources/static/images/profileImg";
		// --------------------
		
		//배포시 사용
		String resourcePath = saveDir;
		

		//UUID로 파일명 설정(중복 방지)
		String originalFileName = mf.getOriginalFilename();
		String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1);
		String savedFileName = "profile_" + UUID.randomUUID().toString() + "." + extension;
		
		re.setImage(savedFileName);
		
		File uploadDir = new File(resourcePath);
		if (!uploadDir.exists()) {
			uploadDir.mkdir();
		}

		File uploadFile = new File(uploadDir, savedFileName);

		mf.transferTo(uploadFile);
	}

}// class
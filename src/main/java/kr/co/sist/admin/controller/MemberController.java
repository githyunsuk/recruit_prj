package kr.co.sist.admin.controller;

import java.util.ArrayList;
import java.util.List;
import kr.co.sist.util.CipherUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.co.sist.admin.Member.MemberEntity;
import kr.co.sist.admin.Member.MemberService;

@Controller
@RequestMapping("/admin")
public class MemberController {
	@Autowired
	private MemberService ms;
	@Autowired
	private CipherUtil cipherUtil;
	

    MemberController(CipherUtil cipherUtil) {
        this.cipherUtil = cipherUtil;
    }

	@GetMapping("/admin_member_detail")
	public String SearchMember(@RequestParam String email,
												Model model) {
		MemberEntity member=ms.searchEmailMember(email);
			try {
       if (member.getPhone() != null) {
           member.setPhone(cipherUtil.decryptText(member.getPhone()));
       }
			} catch (Exception e) {
				e.printStackTrace();
			}  // 복호화
		// filtered 리스트를 모델에 추가해서 뷰에 전달
		model.addAttribute("member", member);
		return "admin/admin_member_detail";
	}

	/*
	@GetMapping("/admin_member_search")
	public String SearchOneMember(@RequestParam("content") String content,@RequestParam("type") String type, Model model) {
		List<MemberEntity> member;
		switch(type) {
			case "name": member=ms.searchNameMember(content); break;
			case "gender": member=ms.searchGenderMember(content); break;
			case "status": member=ms.searchStatusMember(content); break;
			default : member=ms.searchAll();
		}
		
		List<MemberEntity> filtered = new ArrayList();
		for (MemberEntity m : member) {
		    String decryptedName="";
		    String decryptedPhone="";
			try {
				decryptedName = cipherUtil.plainText(m.getName());
				decryptedPhone = cipherUtil.plainText(m.getPhone());
				m.setName(decryptedName); 
				m.setPhone(decryptedPhone); 
			} catch (Exception e) {
				e.printStackTrace();
			}  // 복호화
		        filtered.add(m); // 조건 맞으면 필터링된 리스트에 추가
		}
		model.addAttribute("member",member);
		return "admin/admin_member";
	}
	*/
	
	@GetMapping("/admin_member")
	public String SelectMember2(Model model) {
		List<MemberEntity> member=ms.searchAll2();
		
		List<MemberEntity> filtered = new ArrayList();
		for (MemberEntity m : member) {
			try {
       if (m.getPhone() != null) {
           m.setPhone(cipherUtil.decryptText(m.getPhone()));
       }
			} catch (Exception e) {
				e.printStackTrace();
			}  // 복호화
		        filtered.add(m); // 조건 맞으면 필터링된 리스트에 추가
		}

		// filtered 리스트를 모델에 추가해서 뷰에 전달
		model.addAttribute("member", filtered);
		return "admin/admin_member";
	}
	
	@GetMapping("/admin_member_search")
	public String SearchMember2(
			@RequestParam(required = false) String email,
			@RequestParam(required = false) String name,
      @RequestParam(required = false) String gender,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) String type,
      Model model) {
		
		List<MemberEntity> member=ms.searchMember(email,name,gender,status,type);
		List<MemberEntity> filtered = new ArrayList();
		for (MemberEntity m : member) {
			try {
				if (m.getPhone() != null) {
					m.setPhone(cipherUtil.decryptText(m.getPhone()));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}  // 복호화
			filtered.add(m); // 조건 맞으면 필터링된 리스트에 추가
		}
		
		// filtered 리스트를 모델에 추가해서 뷰에 전달
		model.addAttribute("member", filtered);
		return "admin/admin_member";
	}
}

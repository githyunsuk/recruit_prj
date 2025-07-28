package kr.co.sist.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.co.sist.admin.Member.MemberEntity;
import kr.co.sist.admin.Member.MemberService;
import kr.co.sist.admin.email.EmailService;
import kr.co.sist.util.CipherUtil;

@Controller
@RequestMapping("/admin")
public class AdminSanctionController {
	@Autowired
	private MemberService ms;
	@Autowired
	private CipherUtil cipherUtil;

	 
	 @GetMapping("/admin_sanction")
		public String SearchResume(@RequestParam String email,
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
			return "admin/admin_sanction";
		}
	 
	 @GetMapping("/admin_sanction_cancel")
		public String sanctionCancel(@RequestParam String email,
				Model model) {
		 ms.sanctionCancel(email);
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

		@Autowired
		private EmailService es;
	 @GetMapping("/send_sanction")
		public String SendSaction(@RequestParam(required = false) String email,
				@RequestParam String name,
				@RequestParam String content,
				RedirectAttributes redirectAttributes) {
		 ms.sanctionMember(email);
	    try {
        es.sendSanctionEmail(name, email, content);
        redirectAttributes.addFlashAttribute("successMsg", "이메일이 정상적으로 발송되었습니다.");
    } catch (MailSendException e) {
        redirectAttributes.addFlashAttribute("errorMsg", "메일 발송 제한에 걸렸습니다. 잠시 후 다시 시도해주세요.");
        // 로그 남기기 가능
        e.printStackTrace();
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMsg", "메일 발송 중 오류가 발생했습니다.");
        e.printStackTrace();
    }
			return "redirect:/admin/admin_member";
		}
	 
}

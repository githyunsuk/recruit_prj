let currentPage = 1;
let isLastPage = false;
let loading = false;

document.addEventListener('DOMContentLoaded', function() {

	document.getElementById('announcement-list').addEventListener('click', (e) => {
		const card = e.target.closest('.announcement-card');
		if (card) {
			const corpNo = card.dataset.corpNo;
			location.href = "/user/job_posting/company_info?corpNo=" + corpNo;
		}
	});

	//스크롤 끝까지 내려갔을 때
	window.addEventListener('scroll', async function() {
		const scrollTop = window.scrollY; //현재 스크롤 위치
		const windowHeight = window.innerHeight; //창 높이
		const docHeight = document.documentElement.scrollHeight; //문서 전체 높이

		if (scrollTop + windowHeight >= docHeight - 10 && !isLastPage && !loading) {
			loading = true;
			await loadMoreCompanies();
		}
	});
});

async function loadMoreCompanies() {
	try {
		currentPage += 1;
		
		const response = await fetch('/mypage/company/' + currentPage);
		
		if (!response.ok) throw new Error("Network error");

		const data = await response.json();
		if (data.length === 0) {
			isLastPage = true;
			return;
		}

		const listContainer = document.querySelector(".announcement-list");

		data.forEach(company => {
			const card = document.createElement("div");
			card.className = "announcement-card";
			card.setAttribute("data-corp-no", company.corpNo);

			card.innerHTML = `
       		<div class="image-wrapper">
			<img src="/images/corpimg/${posting.corpLogo}" alt="기업 이미지">
        	</div>
        	<div class="announcement-title">
          	<strong>${company.corpNm}</strong>
        	</div>
     	 	`;

			listContainer.appendChild(card);
		});

	} catch (err) {
		console.error("불러오기 실패:", err);
	} finally{
		loading = false
	}
}

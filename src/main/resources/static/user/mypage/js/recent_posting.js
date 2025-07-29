let currentPage = 1;
let isLastPage = false;
let loading = false;

document.addEventListener('DOMContentLoaded', function() {

	document.getElementById('announcement-list').addEventListener('click', (e) => {
		const card = e.target.closest('.announcement-card');
		if (card) {
			const postingSeq = card.dataset.postingSeq;
			location.href = "/user/job_posting/job_posting_detail?jobPostingSeq=" + postingSeq;
		}
	});

	//스크롤 끝까지 내려갔을 때
	window.addEventListener('scroll', async function() {
		const scrollTop = window.scrollY; //현재 스크롤 위치
		const windowHeight = window.innerHeight; //창 높이
		const docHeight = document.documentElement.scrollHeight; //문서 전체 높이

		if (scrollTop + windowHeight >= docHeight - 10 && !isLastPage && !loading) {
			loading = true;
			await loadMorePostings();
		}
	});
});

async function loadMorePostings() {
	try {
		currentPage += 1;
		
		const response = await fetch('/mypage/scrap/resent/' + currentPage);
		
		if (!response.ok) throw new Error("Network error");

		const data = await response.json();
		if (data.length === 0) {
			isLastPage = true;
			return;
		}

		const listContainer = document.querySelector(".announcement-list");

		data.forEach(posting => {
			const card = document.createElement("div");
			card.className = "announcement-card";
			if (posting.dday < 0) card.classList.add("expired-card");
			card.setAttribute("data-posting-seq", posting.jobPostingSeq);

			card.innerHTML = `
       		<div class="image-wrapper">
			<img src="/images/corpimg/${posting.corpImg}" alt="기업 이미지">
        	</div>
        	<div class="dday-badge ${getBadgeClass(posting.dday)}">
          	<span>${posting.ddayDisplay}</span>
        	</div>
        	<div class="company-name">${posting.corpNm}</div>
        	<div class="announcement-title">
          	<strong>${posting.postingTitle}</strong>
        	</div>
        	<div class="stack-name">${posting.stackName}</div>
        	<div class="company-info">${posting.region} ${posting.district} | ${posting.expLevel}</div>
        	<div class="position-name">${posting.positionName}</div>
     	 	`;

			listContainer.appendChild(card);
		});

	} catch (err) {
		console.error("불러오기 실패:", err);
	} finally{
		loading = false
	}
}

function getBadgeClass(dday) {
	if (dday === 0) return "dday";
	if (dday < 0) return "expired";
	if (dday <= 3) return "urgent";
	return "normal";
}
const chatForm = document.getElementById("chatForm");
const chatInput = document.getElementById("chatInput");
const sendButton = document.getElementById("sendButton");
const messages = document.getElementById("messages");
const emptyState = document.getElementById("emptyState");
const suggestionCards = document.querySelectorAll(".suggestion-card");

let isLoading = false;

function autoResizeTextarea() {
	chatInput.style.height = "auto";
	chatInput.style.height = `${Math.min(chatInput.scrollHeight, 220)}px`;
}

function updateComposerState() {
	sendButton.disabled = isLoading || chatInput.value.trim().length === 0;
}

function toggleEmptyState() {
	emptyState.classList.toggle("is-hidden", messages.childElementCount > 0);
}

function scrollMessagesToBottom() {
	window.requestAnimationFrame(() => {
		window.scrollTo({
			top: document.body.scrollHeight,
			behavior: "smooth"
		});
	});
}

function buildMetaText(response) {
	const metaParts = [];

	if (response.usedDatabase) {
		metaParts.push("DB 조회 기반 답변");
	}

	if (typeof response.resultCount === "number" && response.resultCount > 0) {
		metaParts.push(`${response.resultCount}건 참조`);
	}

	return metaParts.join(" · ");
}

function createLoadingBubble() {
	const wrapper = document.createElement("div");
	wrapper.className = "loading-dots";

	for (let index = 0; index < 3; index += 1) {
		wrapper.append(document.createElement("span"));
	}

	return wrapper;
}

function appendMessage(role, text, options = {}) {
	const row = document.createElement("article");
	row.className = `message-row ${role}`;

	const stack = document.createElement("div");
	stack.className = "message-stack";

	const label = document.createElement("div");
	label.className = "message-label";
	label.textContent = role === "user" ? "You" : "Assistant";

	const bubble = document.createElement("div");
	bubble.className = "message-bubble";

	if (options.loading) {
		bubble.append(createLoadingBubble());
	}
	else {
		bubble.textContent = text;
	}

	stack.append(label, bubble);

	if (options.meta) {
		const meta = document.createElement("div");
		meta.className = "message-meta";
		meta.textContent = options.meta;
		stack.append(meta);
	}

	if (options.error) {
		const error = document.createElement("div");
		error.className = "message-error";
		error.textContent = options.error;
		stack.append(error);
	}

	row.append(stack);
	messages.append(row);
	toggleEmptyState();
	scrollMessagesToBottom();

	return row;
}

async function requestChat(question) {
	const response = await fetch("/api/chat", {
		method: "POST",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify({ question })
	});

	const contentType = response.headers.get("content-type") || "";
	const payload = contentType.includes("application/json")
		? await response.json()
		: { message: await response.text() };

	if (!response.ok) {
		const errorMessage = payload.message || "요청 처리 중 오류가 발생했습니다.";
		throw new Error(errorMessage);
	}

	return payload;
}

async function handleSubmit(event) {
	event.preventDefault();

	const question = chatInput.value.trim();
	if (!question || isLoading) {
		return;
	}

	appendMessage("user", question);
	chatInput.value = "";
	autoResizeTextarea();
	isLoading = true;
	updateComposerState();

	const loadingRow = appendMessage("assistant", "", { loading: true });

	try {
		const response = await requestChat(question);
		loadingRow.remove();
		appendMessage("assistant", response.answer, {
			meta: buildMetaText(response)
		});
	}
	catch (error) {
		loadingRow.remove();
		appendMessage("assistant", "답변을 가져오지 못했습니다.", {
			error: error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다."
		});
	}
	finally {
		isLoading = false;
		updateComposerState();
		chatInput.focus();
	}
}

chatForm.addEventListener("submit", handleSubmit);

chatInput.addEventListener("input", () => {
	autoResizeTextarea();
	updateComposerState();
});

chatInput.addEventListener("keydown", (event) => {
	if (event.key === "Enter" && !event.shiftKey) {
		event.preventDefault();
		chatForm.requestSubmit();
	}
});

suggestionCards.forEach((card) => {
	card.addEventListener("click", () => {
		chatInput.value = card.dataset.prompt || "";
		autoResizeTextarea();
		updateComposerState();
		chatInput.focus();
	});
});

autoResizeTextarea();
updateComposerState();

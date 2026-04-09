package kr.ac.ajou.ie.processchatbot.controller;

import jakarta.validation.Valid;
import kr.ac.ajou.ie.processchatbot.dto.ChatRequest;
import kr.ac.ajou.ie.processchatbot.dto.ChatResponse;
import kr.ac.ajou.ie.processchatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@PostMapping("/chat")
	public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
		return this.chatService.chat(request);
	}
}

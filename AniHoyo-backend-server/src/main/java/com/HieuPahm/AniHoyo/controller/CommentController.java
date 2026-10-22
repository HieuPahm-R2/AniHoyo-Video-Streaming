package com.HieuPahm.AniHoyo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.HieuPahm.AniHoyo.model.dtos.CommentReq;
import com.HieuPahm.AniHoyo.model.dtos.CommentRes;
import com.HieuPahm.AniHoyo.model.entities.Comment;
import com.HieuPahm.AniHoyo.repository.CommentReactRepository;
import com.HieuPahm.AniHoyo.repository.CommentRepository;
import com.HieuPahm.AniHoyo.services.implement.CommentService;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final SimpMessagingTemplate messagingTemplate;
    private final CommentService commentService;
    private final CommentRepository commentRepo;
    private final CommentReactRepository likeRepo;

    public CommentController(CommentService commentService, CommentRepository commentRepo,
            CommentReactRepository likeRepo, SimpMessagingTemplate messagingTemplate) {
        this.commentService = commentService;
        this.commentRepo = commentRepo;
        this.likeRepo = likeRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/{ssId}")
    public List<CommentRes> getComments(@PathVariable Long ssId) {
        return commentService.getComments(ssId);
    }

    @PostMapping("/post/{ssId}")
    public CommentRes addComment(@PathVariable Long ssId, @RequestBody CommentReq req) {
        return commentService.addComment(ssId, req, null);
    }

    @MessageMapping("/comment.post")
    public void postComment(@Payload CommentReq req) {
        Comment parent = null;
        if (req.getParentId() != null) {
            parent = commentRepo.findById(req.getParentId()).orElse(null);
        }

        CommentRes newComment = commentService.addComment(req.getSeason().getId(), req, parent);
        // Gửi comment mới tới tất cả client đang subscribe kênh của season đó
        messagingTemplate.convertAndSend("/topic/comments/" + req.getSeason().getId(), newComment);
    }

    // Xử lý khi có người like/unlike comment
    @MessageMapping("/comment.like")
    public void likeComment(@Payload Map<String, Long> payload) {
        Long commentId = payload.get("commentId");
        Long userId = payload.get("userId");

        Comment comment = commentRepo.findById(commentId).orElseThrow();
        commentService.toggleLike(comment, userId);

        // Lấy số like mới nhất
        long likeCount = commentService.getLikeCount(commentId);
        Map<String, Object> response = Map.of("commentId", commentId, "likeCount", likeCount);
        // Gửi thông tin like mới tới tất cả client
        messagingTemplate.convertAndSend("/topic/comments/like", response);
    }

    @PostMapping("/{ssId}/{parentId}/reply")
    public CommentRes reply(@PathVariable Long ssId, @PathVariable Long parentId, @RequestBody CommentReq req) {
        Comment parent = commentRepo.findById(parentId).orElseThrow();
        return commentService.addComment(ssId, req, parent);
    }

    @PostMapping("/{commentId}/like")
    public void like(@PathVariable Long commentId, @PathVariable Long userId) {

        Comment comment = commentRepo.findById(commentId).orElseThrow();
        commentService.toggleLike(comment, userId);
    }
}

package com.HieuPahm.AniHoyo.services.implement;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.HieuPahm.AniHoyo.model.dtos.CommentReq;
import com.HieuPahm.AniHoyo.model.dtos.CommentRes;
import com.HieuPahm.AniHoyo.model.entities.Comment;
import com.HieuPahm.AniHoyo.model.entities.CommentReact;
import com.HieuPahm.AniHoyo.model.entities.Season;
import com.HieuPahm.AniHoyo.model.entities.User;
import com.HieuPahm.AniHoyo.repository.CommentReactRepository;
import com.HieuPahm.AniHoyo.repository.CommentRepository;
import com.HieuPahm.AniHoyo.repository.SeasonRepository;
import com.HieuPahm.AniHoyo.repository.UserRepository;

@Service
public class CommentService {
    private final NotificationService notificationService;
    private final CommentRepository commentRepository;
    private final CommentReactRepository comReactRepo;
    private final UserRepository userRepository;
    private final SeasonRepository seasonRepository;

    public CommentService(NotificationService notificationService, UserRepository userRepository,
            CommentRepository commentRepository, CommentReactRepository comReactRepo,
            SeasonRepository seasonRepository) {
        this.notificationService = notificationService;
        this.commentRepository = commentRepository;
        this.comReactRepo = comReactRepo;
        this.userRepository = userRepository;
        this.seasonRepository = seasonRepository;
    }

    public List<CommentRes> getComments(Long ssId) {
        List<Comment> roots = commentRepository.findBySeasonIdAndParentIsNullOrderByCreatedAtDesc(ssId);
        return roots.stream().map(c -> this.mapToResponse(c)).collect(Collectors.toList());
    }

    public CommentRes addComment(Long ssId, CommentReq req, Comment parent) {
        User user = this.userRepository.findById(req.getUser().getId()).orElseThrow(
                () -> new NoSuchElementException("Not Found!"));
        Season season = this.seasonRepository.findById(ssId).orElseThrow(
                () -> new NoSuchElementException("Not Found!"));
        Comment c = new Comment(season, user, req.getContent(), parent);
        commentRepository.save(c);

        // Nếu là reply thì gửi notification cho chủ comment gốc
        if (parent != null && !parent.getUser().getId().equals(user.getId())) {
            notificationService.sendNotification(
                    parent.getUser().getId(),
                    user.getId(),
                    "COMMENT_REPLY",
                    c.getId(),
                    user.getFullName() + " đã phản hồi comment của bạn");
        }
        return mapToResponse(c);
    }

    public void toggleLike(Comment comment, Long id) {
        User user = this.userRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("Not Found!"));
        if (comReactRepo.existsByCommentIdAndUserId(comment.getId(), user.getId())) {
            // Unlike
            comReactRepo.deleteAll(
                    comReactRepo.findAll().stream()
                            .filter(l -> l.getComment().getId().equals(comment.getId())
                                    && l.getUser().getId().equals(user.getId()))
                            .toList());
        } else {
            // Like
            comReactRepo.save(new CommentReact(null, comment, user, null));

            if (!comment.getUser().getId().equals(user.getId())) {
                notificationService.sendNotification(
                        comment.getUser().getId(),
                        user.getId(),
                        "COMMENT_LIKE",
                        comment.getId(),
                        user.getFullName() + " đã thích bình luận của bạn");
            }
        }
    }

    private CommentRes mapToResponse(Comment c) {
        return new CommentRes(
                c.getId(),
                c.getUser().getId(),
                c.getUser().getFullName(),
                c.getContent(),
                (int) comReactRepo.countByCommentId(c.getId()),
                c.getReplies().stream().map(x -> this.mapToResponse(x)).collect(Collectors.toList()),
                c.getCreatedAt());
    }

    public long getLikeCount(Long commentId) {
        return comReactRepo.countByCommentId(commentId);
    }
}

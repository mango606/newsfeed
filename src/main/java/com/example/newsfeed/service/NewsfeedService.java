package com.example.newsfeed.service;

import com.example.newsfeed.model.Post;
import com.example.newsfeed.model.User;
import com.example.newsfeed.repository.PostRepository;
import com.example.newsfeed.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsfeedService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String NEWSFEED_CACHE_KEY = "newsfeed:";
    private static final int CACHE_EXPIRE_MINUTES = 5;

    /**
     * 뉴스 피드 생성
     */
    @Transactional(readOnly = true)
    public Page<Post> getNewsfeed(Long userId, int page, int size) {
        log.debug("Fetching newsfeed for user: {}", userId);

        // 캐시 확인
        String cacheKey = NEWSFEED_CACHE_KEY + userId + ":" + page;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);

        // 팔로잉하는 사용자들의 포스트 조회
        List<User> followingUsers = new ArrayList<>(user.getFollowing());
        followingUsers.add(user); // 자신의 포스트도 포함

        if (followingUsers.isEmpty()) {
            return Page.empty(pageable);
        }

        return postRepository.findByUserInOrderByCreatedAtDesc(followingUsers, pageable);
    }

    /**
     * 포스트 작성
     */
    @Transactional
    public Post createPost(Long userId, String content, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = Post.builder()
                .user(user)
                .content(content)
                .imageUrl(imageUrl)
                .build();

        Post savedPost = postRepository.save(post);

        // 캐시 무효화
        invalidateNewsfeedCache(userId);

        return savedPost;
    }

    /**
     * 팔로우
     */
    @Transactional
    public void followUser(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("Following user not found"));

        follower.getFollowing().add(following);
        userRepository.save(follower);

        // 캐시 무효화
        invalidateNewsfeedCache(followerId);
    }

    /**
     * 언팔로우
     */
    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("Following user not found"));

        follower.getFollowing().remove(following);
        userRepository.save(follower);

        // 캐시 무효화
        invalidateNewsfeedCache(followerId);
    }

    /**
     * 좋아요
     */
    @Transactional
    public void likePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);
    }

    /**
     * 캐시 무효화
     */
    private void invalidateNewsfeedCache(Long userId) {
        String pattern = NEWSFEED_CACHE_KEY + userId + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
}
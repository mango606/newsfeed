package com.example.newsfeed.controller;

import com.example.newsfeed.model.Post;
import com.example.newsfeed.model.User;
import com.example.newsfeed.service.NewsfeedService;
import com.example.newsfeed.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final UserService userService;
    private final NewsfeedService newsfeedService;

    @GetMapping("/")
    public String index(Model model) {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            return "redirect:/setup";
        }

        // 첫 번째 사용자로 로그인한 것으로 가정
        User currentUser = users.get(0);
        return "redirect:/feed/" + currentUser.getId();
    }

    @GetMapping("/setup")
    public String setup() {
        return "setup";
    }

    @PostMapping("/setup")
    public String createInitialData() {
        // 샘플 사용자 생성
        User alice = userService.createUser("alice", "Alice Kim", "좋은 하루 되세요! 🌟");
        User bob = userService.createUser("bob", "Bob Lee", "개발자입니다.");
        User charlie = userService.createUser("charlie", "Charlie Park", "여행을 좋아합니다 ✈️");
        User david = userService.createUser("david", "David Choi", "커피 애호가 ☕");

        // 팔로우 관계 설정
        newsfeedService.followUser(alice.getId(), bob.getId());
        newsfeedService.followUser(alice.getId(), charlie.getId());
        newsfeedService.followUser(bob.getId(), alice.getId());
        newsfeedService.followUser(bob.getId(), david.getId());
        newsfeedService.followUser(charlie.getId(), alice.getId());

        // 샘플 포스트 생성
        newsfeedService.createPost(alice.getId(), "안녕하세요! 첫 포스트입니다 😊", null);
        newsfeedService.createPost(bob.getId(), "Spring Boot 너무 좋아요! 오늘 새로운 기능 구현했습니다.", null);
        newsfeedService.createPost(charlie.getId(), "제주도 여행 다녀왔어요! 날씨가 정말 좋았습니다 🌴", null);
        newsfeedService.createPost(david.getId(), "오늘의 커피: 에티오피아 예가체프 ☕", null);
        newsfeedService.createPost(alice.getId(), "저녁 메뉴 추천 받습니다!", null);
        newsfeedService.createPost(bob.getId(), "코딩하다가 막혔는데 해결했어요! 뿌듯합니다 💪", null);

        return "redirect:/";
    }

    @GetMapping("/feed/{userId}")
    public String newsfeed(@PathVariable Long userId,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        User currentUser = userService.getUserById(userId);
        Page<Post> posts = newsfeedService.getNewsfeed(userId, page, 10);
        List<User> allUsers = userService.getAllUsers();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("posts", posts);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("currentPage", page);

        return "newsfeed";
    }

    @PostMapping("/post")
    @ResponseBody
    public String createPost(@RequestParam Long userId,
                             @RequestParam String content) {
        newsfeedService.createPost(userId, content, null);
        return "success";
    }

    @PostMapping("/follow")
    @ResponseBody
    public String follow(@RequestParam Long followerId,
                         @RequestParam Long followingId) {
        newsfeedService.followUser(followerId, followingId);
        return "success";
    }

    @PostMapping("/unfollow")
    @ResponseBody
    public String unfollow(@RequestParam Long followerId,
                           @RequestParam Long followingId) {
        newsfeedService.unfollowUser(followerId, followingId);
        return "success";
    }

    @PostMapping("/like")
    @ResponseBody
    public String like(@RequestParam Long postId) {
        newsfeedService.likePost(postId);
        return "success";
    }

    @GetMapping("/users")
    public String users(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users";
    }
}
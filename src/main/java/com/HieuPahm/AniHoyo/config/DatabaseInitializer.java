package com.HieuPahm.AniHoyo.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.HieuPahm.AniHoyo.model.entities.Category;
import com.HieuPahm.AniHoyo.model.entities.Permission;
import com.HieuPahm.AniHoyo.model.entities.Role;
import com.HieuPahm.AniHoyo.model.entities.Tag;
import com.HieuPahm.AniHoyo.model.entities.User;
import com.HieuPahm.AniHoyo.repository.CategoryRepository;
import com.HieuPahm.AniHoyo.repository.PermissionRepository;
import com.HieuPahm.AniHoyo.repository.RoleRepository;
import com.HieuPahm.AniHoyo.repository.TagRepository;
import com.HieuPahm.AniHoyo.repository.UserRepository;

@Service
public class DatabaseInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(RoleRepository roleRepository, PermissionRepository permissionRepository,
            UserRepository userRepository, CategoryRepository categoryRepository, TagRepository tagRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>>>>>>>> INITIAL DATABASE BEGINS:");
        long countPermission = this.permissionRepository.count();
        long countRole = this.roleRepository.count();
        long countUser = this.userRepository.count();
        long countCategory = this.categoryRepository.count();
        long countTag = this.tagRepository.count();

        if (countPermission == 0) {
            ArrayList<Permission> arrResult = new ArrayList<>();
            arrResult.add(new Permission("Create a film ", "/api/v1/add-film", "POST", "FILMS"));
            arrResult.add(new Permission("Update a film", "/api/v1/update-film", "PUT", "FILMS"));
            arrResult.add(new Permission("Delete a film ", "/api/v1/delete-film/{id}", "DELETE", "FILMS"));
            arrResult.add(new Permission("Get film by id", "/api/v1/film/{id}", "GET", "FILMS"));
            arrResult.add(new Permission("Get films with pagination", "/api/v1/films", "GET", "FILMS"));

            arrResult.add(new Permission("Create a season ", "/api/v1/add-season", "POST", "SEASONS"));
            arrResult.add(new Permission("Update a season", "/api/v1/update-season", "PUT", "SEASONS"));
            arrResult.add(new Permission("Delete a season ", "/api/v1/delete-season/{id}", "DELETE", "SEASONS"));
            arrResult.add(new Permission("Get season by id", "/api/v1/season/{id}", "GET", "SEASONS"));
            arrResult.add(new Permission("Get seasons with pagination", "/api/v1/seasons", "GET", "SEASONS"));
            arrResult.add(
                    new Permission("fetch all seasons of film", "/api/v1/seasons/by-film/{filmId}", "GET", "SEASONS"));
            arrResult.add(new Permission("fetch top 5 of film", "/api/v1/seasons/top-views", "GET", "SEASONS"));
            arrResult.add(
                    new Permission("fetch seasons related", "/api/v1/seasons/related/{seasonId}", "GET", "SEASONS"));

            arrResult.add(new Permission("Create a tag", "/api/v1/add-tag", "POST", "TAGS"));
            arrResult.add(new Permission("Update a tag", "/api/v1/update-tag", "PUT", "TAGS"));
            arrResult.add(new Permission("Delete a tag", "/api/v1/delete-tag/{id}", "DELETE", "TAGS"));
            arrResult.add(new Permission("Get tags with pagination", "/api/v1/tags", "GET", "TAGS"));

            arrResult.add(new Permission("Create a permission", "/api/v1/add-permission", "POST", "PERMISSIONS"));
            arrResult.add(new Permission("Update a permission", "/api/v1/update-permission", "PUT", "PERMISSIONS"));
            arrResult.add(
                    new Permission("Delete a permission", "/api/v1/delete-permission/{id}", "DELETE", "PERMISSIONS"));
            arrResult.add(new Permission("Get a permission by id", "/api/v1/permission/{id}", "GET", "PERMISSIONS"));
            arrResult
                    .add(new Permission("Get permission with pagination", "/api/v1/permissions", "GET", "PERMISSIONS"));

            arrResult.add(new Permission("Create a role", "/api/v1/add-role", "POST", "ROLES"));
            arrResult.add(new Permission("Update a role", "/api/v1/update-role", "PUT", "ROLES"));
            arrResult.add(new Permission("Delete a role", "/api/v1/delete-role/{id}", "DELETE", "ROLES"));
            arrResult.add(new Permission("Get role by id", "/api/v1/role/{id}", "GET", "ROLES"));
            arrResult.add(new Permission("Get roles with pagination", "/api/v1/roles", "GET", "ROLES"));

            arrResult.add(new Permission("Create a user", "/api/v1/add-user", "POST", "USERS"));
            arrResult.add(new Permission("Update a user", "/api/v1/update-user", "PUT", "USERS"));
            arrResult.add(new Permission("Delete a user", "/api/v1/delete-user/{id}", "DELETE", "USERS"));
            arrResult.add(new Permission("Get a user by id", "/api/v1/user/{id}", "GET", "USERS"));
            arrResult.add(new Permission("Get users with pagination", "/api/v1/users", "GET", "USERS"));

            arrResult.add(new Permission("Create a category", "/api/v1/add-category", "POST", "CATEGORIES"));
            arrResult.add(new Permission("Update a category", "/api/v1/update-category", "PUT", "CATEGORIES"));
            arrResult.add(new Permission("Delete a category", "/api/v1/delete-category/{id}", "DELETE", "CATEGORIES"));
            arrResult.add(new Permission("Get categories with pagination", "/api/v1/categories", "GET", "CATEGORIES"));

            arrResult.add(new Permission("Create a episode", "/api/v1/add-episode", "POST", "EPISODES"));
            arrResult.add(new Permission("Update a episode", "/api/v1/update-episode", "PUT", "EPISODES"));
            // Was "/api/v1/delete-epsiode/{id}" (typo) while EpisodeController maps
            // "/delete-episode/{id}", so no role could ever delete an episode.
            arrResult.add(new Permission("Delete a episode", "/api/v1/delete-episode/{id}", "DELETE", "EPISODES"));
            arrResult.add(
                    new Permission("fetch all eps of season", "/api/v1/episodes/by-season/{seasonId}", "GET",
                            "EPISODES"));

            arrResult.add(new Permission("Upload video", "/api/v1/upload/video", "POST", "EPISODES"));
            arrResult.add(new Permission("Upload file", "/api/v1/files", "POST", "FILES"));

            arrResult.add(new Permission("Post a comment", "/api/v1/comments/post/{ssId}", "POST", "COMMENTS"));
            arrResult.add(new Permission("Read comments", "/api/v1/comments/{ssId}", "GET", "COMMENTS"));
            arrResult.add(
                    new Permission("Reply a comment", "/api/v1/comments/{ssId}/{parentId}/reply", "POST", "COMMENTS"));
            arrResult.add(new Permission("React to comment", "/api/v1/comments/{commentId}/like", "POST", "COMMENTS"));

            this.permissionRepository.saveAll(arrResult);
        }
        if (countRole == 0) {
            List<Permission> permissions = this.permissionRepository.findAll();

            Role initRole = new Role();
            initRole.setName("ADMIN");
            initRole.setDescription("Contain full of permissions on this web service");
            initRole.setActive(true);
            initRole.setPermissions(permissions);

            this.roleRepository.save(initRole);
        }
        if (countUser == 0) {
            User initUser = new User();
            initUser.setFullName("HieuPahmR2");
            initUser.setEmail("admin@gmail.com");
            initUser.setPassword(this.passwordEncoder.encode("123456"));

            Role userRole = this.roleRepository.findByName("ADMIN");
            if (userRole != null) {
                initUser.setRole(userRole);
            }
            this.userRepository.save(initUser);
        }
        // Reference data for the "add film" form: a stable list of genres
        // (categories) and highlight tags. Seeded once, only while the table is
        // empty, so anything edited/added in the admin UI (table-category /
        // table-tag) is respected on later restarts.
        if (countCategory == 0) {
            List<String> genreNames = List.of(
                    "Hành Động", "Phiêu Lưu", "Hài Hước", "Học Đường", "Kỳ Ảo", "Kinh Dị",
                    "Lãng Mạn", "Mecha", "Âm Nhạc", "Bí Ẩn", "Đời Thường", "Thể Thao",
                    "Siêu Nhiên", "Isekai", "Shounen", "Seinen", "Josei", "Shoujo");
            List<Category> categories = genreNames.stream().map(name -> {
                Category category = new Category();
                category.setName(name);
                return category;
            }).collect(Collectors.toList());
            this.categoryRepository.saveAll(categories);
            System.out.println(">>>>>>>>> SEEDED " + categories.size() + " film categories");
        }
        if (countTag == 0) {
            List<String> tagNames = List.of(
                    "Hot", "Vietsub", "Thuyết Minh", "Full HD", "Trọn Bộ", "Độc Quyền");
            List<Tag> tags = tagNames.stream().map(name -> {
                Tag tag = new Tag();
                tag.setName(name);
                return tag;
            }).collect(Collectors.toList());
            this.tagRepository.saveAll(tags);
            System.out.println(">>>>>>>>> SEEDED " + tags.size() + " film tags");
        }
        if (countRole > 0 && countPermission > 0 && countUser > 0 && countCategory > 0 && countTag > 0) {
            System.out.println("SKIP INITIAL DATABASE");
        } else {
            System.out.println("END TASK");
        }
    }
}

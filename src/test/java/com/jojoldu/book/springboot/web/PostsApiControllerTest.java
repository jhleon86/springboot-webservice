package com.jojoldu.book.springboot.web;

import com.jojoldu.book.springboot.domain.posts.Posts;
import com.jojoldu.book.springboot.domain.posts.PostsRepository;
import com.jojoldu.book.springboot.service.posts.PostsService;
import com.jojoldu.book.springboot.web.dto.PostsResponseDto;
import com.jojoldu.book.springboot.web.dto.PostsSaveRequestDto;
import com.jojoldu.book.springboot.web.dto.PostsUpdateRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PostsApiControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private PostsService postsService;

    @AfterEach
    public void cleanup() {
        postsRepository.deleteAll();
    }


    @Test
    public void Posts_등록된다(){
        //given
        String title = "title";
        String content = "content";

        PostsSaveRequestDto postsSaveRequestDto = PostsSaveRequestDto.builder()
                .title(title)
                .content(content)
                .author("author")
                .build();

        String url = "http://localhost:" + port + "/api/v1/posts";

        //when
        ResponseEntity<Long> responseEntity = restTemplate.postForEntity(url,postsSaveRequestDto,Long.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isGreaterThan(0L);

        List<Posts> postsList = postsRepository.findAll();
        Posts posts = postsList.get(0);
        assertThat(posts.getTitle()).isEqualTo(title);
        assertThat(posts.getContent()).isEqualTo(content);
    }

    @Test
    public void Posts_수정된다(){

        //given
        String title = "title";
        String content = "content";

        Posts savedPosts = postsRepository.save(Posts.builder()
                .title(title)
                .content(content)
                .author("j")
                .build());

        Long updateId = savedPosts.getId();
        String expectedTitle = "title2";
        String expectedContent = "content2";

        PostsUpdateRequestDto requestDto = PostsUpdateRequestDto.builder()
                .title(expectedTitle)
                .content(expectedContent)
                .build();

        String url = "http://localhost:" + port + "/api/v1/posts/"+updateId;

        ResponseEntity<PostsResponseDto> responseEntityGet = restTemplate.getForEntity(url,PostsResponseDto.class);
        assertThat(responseEntityGet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntityGet.getBody().getId()).isEqualTo(updateId);


        //--exchange--
        //
        HttpEntity<PostsUpdateRequestDto> requestEntity = new HttpEntity<>(requestDto);

        ResponseEntity<Long> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Long.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isGreaterThan(0L);

        List<Posts> all = postsRepository.findAll();
        assertThat(all.get(0).getTitle()).isEqualTo(expectedTitle);
        assertThat(all.get(0).getContent()).isEqualTo(expectedContent);
    }

    @Test
    public void BaseTimeEntity_등록(){
        //given
        LocalDateTime now = LocalDateTime.now();

        Long id = postsRepository.save(Posts.builder().title("title").content("content").author("j").build()).getId();

        List<Posts> postsList = postsRepository.findAll();

        System.out.println(">>>>>>>>>>> nowDate="+now);
        System.out.println(">>>>>>>>>>> createDate="+postsList.get(0).getCreateDate()+" modifiedDate="+postsList.get(0).getModifiedDate());

        assertThat(postsList.get(0).getCreateDate()).isAfter(now);
        assertThat(postsList.get(0).getModifiedDate()).isAfter(now);


//        Posts posts = postsRepository.findById(id).orElseThrow();
//        posts.update("title2", "content2");  >> 트랜젝션 내부가 아니기때문에 영속성 컨텍스트가 유지되지 않음

        PostsUpdateRequestDto requestDto = PostsUpdateRequestDto.builder().title("title2").content("content2").build();
        postsService.update(id, requestDto);
        postsList = postsRepository.findAll();
        System.out.println(">>>>>>>>>>> createDate="+postsList.get(0).getCreateDate()+" modifiedDate="+postsList.get(0).getModifiedDate());
        assertThat(postsList.get(0).getModifiedDate()).isAfter(postsList.get(0).getCreateDate());
    }
}

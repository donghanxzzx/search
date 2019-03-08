package com.dhxz.search.service;

import static com.dhxz.search.exception.ExceptionEnum.BOOK_NOT_FOUND;
import static com.dhxz.search.exception.ExceptionEnum.CHAPTER_NOT_COMPLETED;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import com.dhxz.search.domain.Content;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.repository.ChapterRepository;
import com.dhxz.search.repository.ContentRepository;
import com.dhxz.search.vo.BookInfoVo;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/**
 * @author 10066610
 * @description 输出流相关服务
 * @date 2019/3/8 15:15
 **/
@Slf4j
@Service
public class OutputStreamService {

    private BookInfoRepository bookInfoRepository;
    private ChapterRepository chapterRepository;
    private ContentRepository contentRepository;

    public OutputStreamService(BookInfoRepository bookInfoRepository,
            ChapterRepository chapterRepository,
            ContentRepository contentRepository) {
        this.bookInfoRepository = bookInfoRepository;
        this.chapterRepository = chapterRepository;
        this.contentRepository = contentRepository;
    }

    /**
     * 下载书籍
     *
     * @param bookInfoVo 书本信息
     * @param response 目标地址
     * @return 文件
     */
    public HttpServletResponse downloadBook(BookInfoVo bookInfoVo, HttpServletResponse response) {
        BookInfo book = bookInfoRepository.findById(bookInfoVo.getId())
                .orElseThrow(BOOK_NOT_FOUND);
        if (chapterRepository.existsByBookInfoAndCompleted(book, false)) {
            throw CHAPTER_NOT_COMPLETED.get();
        }
        List<Chapter> chapters = chapterRepository
                .findByBookInfoIdOrderByChapterOrderAsc(book.getId());
        StringBuilder sb = new StringBuilder();
        chapters.forEach(item -> {
            Optional<Content> optional = contentRepository.findById(item.getContent().getId());
            if (optional.isPresent()) {
                String content = optional.get().getContent();
                content = "\t" + content + "\r\n";
                sb.append(content);
            }
        });
        try {
            String title = book.getTitle() + ".txt";
            byte[] bytes = sb.toString().getBytes();
            response.reset();
            response.resetBuffer();
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;fileName=" + URLEncoder.encode(title, "UTF-8"));
            response.addHeader(HttpHeaders.CONTENT_LENGTH, "" + bytes.length);
            BufferedOutputStream bos = new BufferedOutputStream(
                    response.getOutputStream());
            response.setContentType("application/octet-stream");
            bos.write(bytes);
            bos.close();
        } catch (IOException ex) {
            log.error("IO异常:", ex);
        }
        return response;
    }
}

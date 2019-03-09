package com.dhxz.search.service;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.repository.ChapterRepository;
import com.dhxz.search.repository.LineRepository;
import com.dhxz.search.repository.PageRepository;
import com.dhxz.search.vo.BookInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;

import static com.dhxz.search.exception.ExceptionEnum.BOOK_NOT_FOUND;
import static com.dhxz.search.exception.ExceptionEnum.CHAPTER_NOT_COMPLETED;

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
    private PageRepository pageRepository;
    private LineRepository lineRepository;

    public OutputStreamService(BookInfoRepository bookInfoRepository,
                               ChapterRepository chapterRepository, PageRepository pageRepository, LineRepository lineRepository) {
        this.bookInfoRepository = bookInfoRepository;
        this.chapterRepository = chapterRepository;
        this.pageRepository = pageRepository;
        this.lineRepository = lineRepository;
    }

    /**
     * 下载书籍
     *
     * @param bookInfoVo 书本信息
     * @param response   目标地址
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
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");

        chapters.stream()
                .map(chapter -> pageRepository.findByChapterIdOrderByPageOrderAsc(chapter.getId()))
                .map(pages -> pages.stream().map(page -> lineRepository.findByPageIdOrderByLineOrderAsc(page.getId()))
                        .map(lines -> lines.stream().peek(line -> {
                            sb.append(line.getContent())
                                    .append("\r\n");
                        }).count()).count()
                ).count();

        try {
            String title = book.getTitle() + ".txt";
            byte[] bytes = sb.toString().getBytes();
            String path = getPath(title);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(path)));
            bos.write(bytes);
            bos.flush();
            bos.close();
            bos = writeToResponseWithoutCloseSource(response, title, bytes);
            bos.close();
        } catch (IOException ex) {
            log.error("IO异常:", ex);
        }
        return response;
    }

    public static String getPath(String title) {
        String separator = File.separator;
        String path = System.getProperty("java.io.tmpdir");
        if (!path.endsWith(separator)) {
            path = path + separator;
        }

        path = path + title;
        return path;
    }

    public void readFromDisk(File file, HttpServletResponse response) {
        try (
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        ) {
            String title = file.getName();
            byte[] buffer = new byte[bis.available()];
            int read = bis.read(buffer);
            writeToResponseWithoutCloseSource(response, title, buffer);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedOutputStream writeToResponseWithoutCloseSource(HttpServletResponse response, String title, byte[] bytes) throws IOException {
        response.reset();
        response.resetBuffer();
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment;fileName=" + URLEncoder.encode(title, "UTF-8"));
        response.addHeader(HttpHeaders.CONTENT_LENGTH, "" + bytes.length);
        BufferedOutputStream bos = new BufferedOutputStream(
                response.getOutputStream());
        response.setContentType("application/octet-stream");
        bos.write(bytes);
        bos.flush();
        bos.close();
        return bos;
    }
}

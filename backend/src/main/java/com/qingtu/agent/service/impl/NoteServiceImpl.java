package com.qingtu.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.entity.po.CourseKeyPoint;
import com.qingtu.agent.entity.po.Notes;
import com.qingtu.agent.mapper.CourseKeyPointMapper;
import com.qingtu.agent.mapper.NotesMapper;
import com.qingtu.agent.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI笔记服务实现类
 */
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final CourseKeyPointMapper courseKeyPointMapper;
    private final NotesMapper notesMapper;

    private static final int SUMMARY_LENGTH = 150;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public CommonResult<?> listNotes(Long userId, Long courseId, Integer weekNum, int page, int size) {
        LambdaQueryWrapper<Notes> wrapper = new LambdaQueryWrapper<Notes>()
                .eq(Notes::getUserId, userId)
                .eq(Notes::getDeleted, 0)
                .orderByDesc(Notes::getCreateTime);

        if (courseId != null) {
            wrapper.eq(Notes::getCourseId, courseId);
        }

        Page<Notes> pageObj = new Page<>(page, size);
        IPage<Notes> result = notesMapper.selectPage(pageObj, wrapper);

        List<Map<String, Object>> records = result.getRecords().stream().map(note -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", note.getId());
            map.put("courseName", note.getTitle());
            map.put("classDate", formatDateTime(note.getCreateTime()));
            map.put("summary", truncateContent(note.getContent(), SUMMARY_LENGTH));
            map.put("content", note.getContent());
            map.put("noteType", note.getNoteType());
            map.put("createTime", formatDateTime(note.getCreateTime()));
            return map;
        }).toList();

        Page<Map<String, Object>> resultPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        resultPage.setRecords(records);

        Map<String, Object> response = new HashMap<>();
        response.put("records", records);
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());

        return CommonResult.success(response);
    }

    @Override
    public CommonResult<?> getNoteById(Long userId, Long noteId) {
        Notes note = notesMapper.selectOne(
                new LambdaQueryWrapper<Notes>()
                        .eq(Notes::getId, noteId)
                        .eq(Notes::getUserId, userId));

        if (note == null) {
            return CommonResult.fail("笔记不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", note.getId());
        result.put("title", note.getTitle());
        result.put("content", note.getContent());
        result.put("noteType", note.getNoteType());
        result.put("courseId", note.getCourseId());
        result.put("createTime", formatDateTime(note.getCreateTime()));
        result.put("updateTime", formatDateTime(note.getUpdateTime()));

        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> getNotesByCourse(Long userId, Long courseId) {
        List<Notes> notes = notesMapper.selectList(
                new LambdaQueryWrapper<Notes>()
                        .eq(Notes::getUserId, userId)
                        .eq(Notes::getCourseId, courseId)
                        .eq(Notes::getDeleted, 0)
                        .orderByDesc(Notes::getCreateTime));

        List<Map<String, Object>> result = notes.stream().map(note -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", note.getId());
            map.put("title", note.getTitle());
            map.put("summary", truncateContent(note.getContent(), SUMMARY_LENGTH));
            map.put("createTime", formatDateTime(note.getCreateTime()));
            return map;
        }).toList();

        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> generateNote(Long userId, Long courseId) {
        CourseKeyPoint note = new CourseKeyPoint();
        note.setUserId(userId);
        note.setCourseId(courseId);
        note.setWeekNum(1);
        note.setClassDate(LocalDate.now());
        note.setCorePoints("[\"核心知识点1\", \"核心知识点2\"]");
        note.setSummary("这是自动生成的课程笔记");
        courseKeyPointMapper.insert(note);
        return CommonResult.success("笔记生成成功");
    }

    @Override
    public CommonResult<?> getDailySummary(Long userId, String date) {
        if (date == null) date = LocalDate.now().toString();

        LambdaQueryWrapper<Notes> wrapper = new LambdaQueryWrapper<Notes>()
                .eq(Notes::getUserId, userId)
                .eq(Notes::getDeleted, 0)
                .likeRight(Notes::getCreateTime, date);

        List<Notes> notes = notesMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("count", notes.size());
        result.put("notes", notes.stream().map(note -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", note.getId());
            map.put("title", note.getTitle());
            map.put("content", note.getContent());
            return map;
        }).toList());

        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> exportNote(Long userId, Long noteId, String format) {
        Notes note = notesMapper.selectOne(
                new LambdaQueryWrapper<Notes>()
                        .eq(Notes::getId, noteId)
                        .eq(Notes::getUserId, userId));

        if (note == null) {
            return CommonResult.fail("笔记不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("title", note.getTitle());
        result.put("content", note.getContent());
        result.put("format", format != null ? format : "markdown");

        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> deleteNote(Long userId, Long noteId) {
        Notes note = notesMapper.selectOne(
                new LambdaQueryWrapper<Notes>()
                        .eq(Notes::getId, noteId)
                        .eq(Notes::getUserId, userId));

        if (note == null) {
            return CommonResult.fail("笔记不存在");
        }

        notesMapper.deleteById(noteId);
        return CommonResult.success("笔记删除成功");
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_FORMATTER);
    }
}

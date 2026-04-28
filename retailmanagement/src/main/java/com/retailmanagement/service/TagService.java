package com.retailmanagement.service;

import com.retailmanagement.entity.Tag;
import com.retailmanagement.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public List<Tag> getAllActiveTags() {
        return tagRepository.findByIsActiveTrue();
    }
}
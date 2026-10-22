package com.HieuPahm.AniHoyo.services;

import java.io.IOException;
import java.net.URISyntaxException;

import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    //create new folder if not exists folder before
    void createFolder(String folderName) throws URISyntaxException;
    // store data
    String storeFile(MultipartFile file, String folderName) throws URISyntaxException, IOException;
}

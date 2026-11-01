/*******************************************************************************
 * Copyright 2017 Bstek
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.bstek.ureport.provider.report.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.bstek.ureport.exception.ReportException;
import com.bstek.ureport.provider.report.ReportFile;
import com.bstek.ureport.provider.report.ReportProvider;

/**
 * @author Jacky.gao
 * @since 2017年2月11日
 */
public class FileReportProvider implements ReportProvider,ApplicationContextAware{
	private String prefix="file:";
	private String fileStoreDir;
	private boolean disabled;
	@Override
	public InputStream loadReport(String file) {
		if(file.startsWith(prefix)){
			file=file.substring(prefix.length(),file.length());
		}
		String fullPath=fileStoreDir+"/"+file;
		try {
			return new FileInputStream(fullPath);
		} catch (FileNotFoundException e) {
			throw new ReportException(e);
		}
	}
	
	@Override
	public void deleteReport(String file) {
		if(file.startsWith(prefix)){
			file=file.substring(prefix.length(),file.length());
		}
		String fullPath=fileStoreDir+"/"+file;
		File f=new File(fullPath);
		if(f.exists()){
			f.delete();
		}
	}

	@Override
	public List<ReportFile> getReportFiles() {
		File file=new File(fileStoreDir);
		List<ReportFile> list=new ArrayList<ReportFile>();
		File[] files=file.listFiles();
		if(files==null){
			return list;
		}
		for(File f:files){
			Calendar calendar=Calendar.getInstance();
			calendar.setTimeInMillis(f.lastModified());
			list.add(new ReportFile(f.getName(),calendar.getTime()));
		}
		Collections.sort(list, new Comparator<ReportFile>(){
			@Override
			public int compare(ReportFile f1, ReportFile f2) {
				return f2.getUpdateDate().compareTo(f1.getUpdateDate());
			}
		});
		return list;
	}

	@Override
	public String getName() {
		return "服务器文件系统";
	}
	
	@Override
	public void saveReport(String file,String content) {
		if(file.startsWith(prefix)){
			file=file.substring(prefix.length(),file.length());
		}
		String fullPath=fileStoreDir+"/"+file;
		FileOutputStream outStream=null;
		try{
			outStream=new FileOutputStream(new File(fullPath));
			IOUtils.write(content, outStream,"utf-8");
		}catch(Exception ex){
			throw new ReportException(ex);
		}finally{
			if(outStream!=null){
				try {
					outStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	@Override
	public boolean disabled() {
		return disabled;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public void setFileStoreDir(String fileStoreDir) {
		this.fileStoreDir = fileStoreDir;
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// yudao-ureport2: 禁用时不解析、不创建任何目录
		if(disabled){
			return;
		}
		if(fileStoreDir==null || fileStoreDir.trim().length()==0){
			throw new ReportException("ureport.fileStoreDir must be configured when the file report provider is enabled.");
		}
		// yudao-ureport2: 启用时只接受绝对目录；原 /WEB-INF/... 为 Web 根相对路径，不再解析，避免写入 Web 根或文件系统根下的 /WEB-INF
		String dir=fileStoreDir.trim();
		if(dir.replace('\\','/').startsWith("/WEB-INF")){
			throw new ReportException("ureport.fileStoreDir ["+dir+"] is a web-root relative path, which is no longer supported. Please configure an absolute directory.");
		}
		File file=new File(dir);
		if(!file.isAbsolute()){
			throw new ReportException("ureport.fileStoreDir ["+dir+"] must be an absolute directory.");
		}
		fileStoreDir=file.getAbsolutePath();
		if(!file.exists() && !file.mkdirs()){
			throw new ReportException("Failed to create ureport.fileStoreDir ["+fileStoreDir+"].");
		}
	}

	@Override
	public String getPrefix() {
		return prefix;
	}
}

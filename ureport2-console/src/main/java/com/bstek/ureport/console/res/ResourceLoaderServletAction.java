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
package com.bstek.ureport.console.res;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import com.bstek.ureport.console.ServletAction;
import com.bstek.ureport.console.UReportServlet;

/**
 * @author Jacky.gao
 * @since 2016年6月6日
 */
public class ResourceLoaderServletAction implements ServletAction,ApplicationContextAware{
	public static final String URL="/res";
	private static final String ASSET_DIR="ureport-asserts/";
	private ApplicationContext applicationContext;
	@Override
	public void execute(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path=req.getContextPath()+UReportServlet.URL+URL;
		String uri=req.getRequestURI();
		String resPath=uri.length()>path.length()?uri.substring(path.length()+1):"";
		// yudao-ureport2: 只允许读取打包的 ureport-asserts 静态资源目录，拒绝路径穿越与其他 classpath 资源
		resPath=checkAssetPath(resPath);
		if(resPath==null){
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		String p="classpath:"+resPath;
		Resource resource=applicationContext.getResource(p);
		if(!resource.exists()){
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if(p.endsWith(".js")){
			resp.setContentType("text/javascript");	
		}else if(p.endsWith(".css")){
			resp.setContentType("text/css");
		}else if(p.endsWith(".png")){
			resp.setContentType("image/png");
		}else if(p.endsWith(".jpg")){
			resp.setContentType("image/jpeg");
		}else if(p.endsWith(".svg")){
			resp.setContentType("image/svg+xml");			
		}else{
			resp.setContentType("application/octet-stream");
		}
		InputStream input=resource.getInputStream();
		OutputStream output=resp.getOutputStream();
		try{
			IOUtils.copy(input, output);			
		}finally{
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(output);
		}
	}

	/**
	 * yudao-ureport2: 校验资源路径，合法时返回解码后的路径，否则返回 null。
	 * 仅接受 ureport-asserts/ 下的普通文件名；拒绝反斜线、分号、除 %20 外的百分号编码以及 . / .. / 空路径段。
	 */
	static String checkAssetPath(String rawPath){
		if(rawPath==null || rawPath.isEmpty()){
			return null;
		}
		if(rawPath.indexOf('\\')>-1 || rawPath.indexOf(';')>-1 || rawPath.indexOf('\u0000')>-1){
			return null;
		}
		String path=rawPath.replace("%20", " ");
		if(path.indexOf('%')>-1){
			return null;
		}
		if(!path.startsWith(ASSET_DIR)){
			return null;
		}
		for(String segment:path.split("/",-1)){
			if(segment.isEmpty() || segment.equals(".") || segment.equals("..")){
				return null;
			}
		}
		return path;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext=applicationContext;
	}
	
	@Override
	public String url() {
		return URL;
	}
}

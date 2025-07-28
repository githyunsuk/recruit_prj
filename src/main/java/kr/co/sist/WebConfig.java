package kr.co.sist;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${upload.saveDir}")
	private String uploadDir;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// 프로필 이미지: /images/profileImg/xxx → /app/upload/xxx
		registry.addResourceHandler("/images/profileImg/**").addResourceLocations("file:" + uploadDir + "/");

		// 기업 이미지: /images/corpimg/xxx → /app/upload/xxx
		registry.addResourceHandler("/images/corpimg/**").addResourceLocations("file:" + uploadDir + "/");

		// 기업 로고: /images/corplogo/xxx → /app/upload/xxx
		registry.addResourceHandler("/images/corplogo/**").addResourceLocations("file:" + uploadDir + "/");
	}

}

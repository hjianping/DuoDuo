package xmlparse;

import elements.info.Bean;
import elements.info.EntityInfo;
import org.qiunet.template.creator.BaseXmlParse;

/**
 * @author qiunet
 *         Created on 17/2/15 18:20.
 */
public class EntityInfoXmlParse extends BaseXmlParse {

	/***
	 * 构造一个 xmlparse 解析 xml

	 * @param basePath	对于xmlfile和 vmfile的一个基础路径, 之后生成文件 和 vm是基于该路径的相对路径.
	 * @param xmlConfigPath xml路径
	xml 第一个元素的名称
	 */
	public EntityInfoXmlParse(String basePath, String xmlConfigPath) {
		super(BeanVmElement.class, basePath, xmlConfigPath);
	}
	@Override
	public void parseXml() {
		addObjectCreate("base/beans/bean", Bean.class, "addBean");
		addObjectCreate("base/infos/info", EntityInfo.class);
	}
}

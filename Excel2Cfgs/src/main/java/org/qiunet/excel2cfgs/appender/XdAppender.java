package org.qiunet.excel2cfgs.appender;


import javafx.scene.control.Alert;
import org.qiunet.excel2cfgs.enums.OutPutType;
import org.qiunet.excel2cfgs.enums.RoleType;
import org.qiunet.excel2cfgs.utils.FxUIUtil;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 拼接xd的方式
 * Created by qiunet.
 * 17/10/30
 */
public class XdAppender extends BaseAppender {

	public XdAppender(String outputRelativePath, String filePrefix) {
		super(outputRelativePath, filePrefix);
	}

	@Override
	public void createCfgFile(String sheetName, RoleType roleType, String outPath, AppenderAttachable attachable) {
		Path path = Paths.get(outPath, outputRelativePath, filePrefix + "_" + sheetName + ".xd");
		if (! path.toFile().getParentFile().exists()) {
			path.toFile().getParentFile().mkdirs();
		}

		List<List<AppenderData>> appenderDatas = attachable.getAppenderDatas();
		try(FileOutputStream fos = new FileOutputStream(path.toFile());
			DataOutputStream dos = new DataOutputStream(fos)) {
			// 写入数据行数
			dos.writeInt(appenderDatas.size());
			// 写入名称
			List<String> names = attachable.getRowNames();
			dos.writeShort(names.size());
			for (String name : names) {
				dos.writeUTF(name);
			}
			// 写入数据
			for (List<AppenderData> rowDatas : appenderDatas) {
				for (AppenderData rowData : rowDatas) {
					OutPutType oType = rowData.getOutPutType();
					if (oType.canWrite(roleType)) {
						dos.writeUTF(rowData.getVal());
					}
				}
			}
		}catch (RuntimeException e) {
			throw e;
		}catch (Exception e) {
			e.printStackTrace();
			FxUIUtil.openAlert(Alert.AlertType.ERROR, e.getMessage(), "错误");
		}finally {
			this.copyToProject(path.toFile());
		}
	}

	@Override
	public void fileOver() {
		// do noting
	}

	@Override
	public String name() {
		return "xd";
	}
}

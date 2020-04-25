package org.qiunet.excel2cfgs.appender;


import com.google.common.collect.Lists;
import javafx.scene.control.Alert;
import org.qiunet.excel2cfgs.enums.OutPutType;
import org.qiunet.excel2cfgs.enums.RoleType;
import org.qiunet.excel2cfgs.utils.FxUIUtil;
import org.qiunet.utils.common.CommonUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPOutputStream;

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
		try (ByteArrayOutputStream bouts = new ByteArrayOutputStream(1024)){
			try(GZIPOutputStream gos = new GZIPOutputStream(bouts);
				DataOutputStream dos = new DataOutputStream(gos)) {
				// 写入数据行数
				dos.writeInt(appenderDatas.size());
				if (appenderDatas.isEmpty()) {
					return;
				}

				// 写入名称
				List<String> names = Lists.newArrayList();
				List<AppenderData> appenderData = appenderDatas.get(0);
				for (AppenderData rowData : appenderData) {
					OutPutType oType = rowData.getOutPutType();
					if (oType.canWrite(roleType)) {
						names.add(rowData.getName());
					}
				}

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
			}

			byte [] bytes = bouts.toByteArray();
			CommonUtil.reverse(bytes);
			try (FileOutputStream fos = new FileOutputStream(path.toFile())){
				fos.write(bytes);
			}
		} catch (RuntimeException e) {
			throw e;
		}catch (Exception e) {
			e.printStackTrace();
			FxUIUtil.openAlert(Alert.AlertType.ERROR, e.getMessage(), "错误");
		}



		this.copyToProject(path.toFile());
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

package org.qiunet.data1.cache;

import org.qiunet.data1.support.IEntityVo;

public class GuildMemberVo implements IEntityVo<GuildMemberPo> {

	private GuildMemberPo guildMemberPo;

	public GuildMemberVo(GuildMemberPo guildMemberPo) {
		this.guildMemberPo = guildMemberPo;
	}
	@Override
	public GuildMemberPo getPo() {
		return guildMemberPo;
	}
}

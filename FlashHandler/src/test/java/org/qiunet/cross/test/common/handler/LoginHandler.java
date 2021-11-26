package org.qiunet.cross.test.common.handler;

import org.qiunet.cross.test.common.event.PlayerLoginEventData;
import org.qiunet.cross.test.common.proto.req.LoginRequest;
import org.qiunet.flash.handler.common.player.PlayerActor;
import org.qiunet.flash.handler.context.request.persistconn.IPersistConnRequest;

/***
 *
 *
 * @author qiunet
 * 2020-10-23 09:54
 */
public class LoginHandler extends BaseHandler<LoginRequest> {
	@Override
	public void handler(PlayerActor playerActor, IPersistConnRequest<LoginRequest> context) throws Exception {
		playerActor.auth(context.getRequestData().getPlayerId());

		playerActor.fireEvent(new PlayerLoginEventData());
	}

	@Override
	public boolean needAuth() {
		return false;
	}
}

package com.telenordigital.horde;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.websocket.*;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

class OutputWebsocketClient extends Endpoint {
	private static final Logger logger = Logger.getLogger(OutputWebsocketClient.class.getName());

	private final String token;
	private Client.OutputHandler handler = null;

	private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
		= new com.fasterxml.jackson.databind.ObjectMapper();

	public OutputWebsocketClient(URI uri, String token, Client.OutputHandler handler) {
		this.token = token;
		this.handler = handler;

		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			ClientEndpointConfig config = ClientEndpointConfig.Builder.create().configurator(new Config()).build();
			container.connectToServer(this, config, uri);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void onOpen(Session session, EndpointConfig config) {
		session.addMessageHandler(new MessageHandler.Whole<String>() {
			@Override
			public void onMessage(String text) {
				try {
					OutputMessage msg = jacksonObjectMapper.readValue(text, OutputMessage.class);
					if (!msg.keepAlive()) {
						handler.onOutput(msg);
					}
				} catch (java.io.IOException e) {
					logger.log(Level.WARNING, "Error parsing message: {0}", e.toString());
					logger.log(Level.WARNING, "Message was: {0}", text);
				}
			}
		});
	}

	public void onClose(Session session, CloseReason reason) {
		handler.onEnd();
	}

	public void onError(Session session, Throwable thr) {
		logger.log(Level.WARNING, "{0}", thr);
	}

	public class Config extends ClientEndpointConfig.Configurator{
		@Override
		public void beforeRequest(Map<String, List<String>> headers) {
			headers.put("X-API-Token", Arrays.asList(token));
		}
	}
}
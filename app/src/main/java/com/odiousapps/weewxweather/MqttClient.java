package com.odiousapps.weewxweather;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;

class MqttClient
{
	private MqttAsyncClient client;

	public void connect(String brokerUrl, String clientId, String username, String password)
	{
		try
		{
			client = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());

			MqttConnectOptions options = new MqttConnectOptions();
			options.setUserName(username);
			options.setPassword(password.toCharArray());
			options.setCleanSession(true);
			options.setAutomaticReconnect(true);
			options.setKeepAliveInterval(60);

			client.setCallback(new MqttCallback() {
			@Override
			public void connectionLost(Throwable cause)
			{
				LogMessage("Connection lost: " + cause.getMessage());
			}

			@Override
			public void messageArrived(String topic, MqttMessage message)
			{
				LogMessage("Topic: " + topic);
				LogMessage("Message: " + new String(message.getPayload()));
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token)
			{
				LogMessage("Delivery complete");
			}
			});

			client.connect(options, null, new IMqttActionListener() {
			@Override
			public void onSuccess(IMqttToken token) {
			System.out.println("Connected");
			subscribe("your/topic");
			}

			@Override
			public void onFailure(IMqttToken token, Throwable e) {
			System.out.println("Connect failed: " + e.getMessage());
			}
			});

		} catch (MqttException e) {
			weeWXAppCommon.doStackOutput(e);
		}
	}

	public void subscribe(String topic)
	{
		try
		{
			client.subscribe(topic, 0); // QoS 1
		} catch (MqttException e) {
			weeWXAppCommon.doStackOutput(e);
		}
	}

	public void publish(String topic, String payload)
	{
	try {
	MqttMessage message = new MqttMessage(payload.getBytes());
	message.setQos(1);
	client.publish(topic, message);
	} catch (MqttException e) {
	e.printStackTrace();
	}
	}

	public void disconnect() {
	try {
	if (client != null && client.isConnected()) {
	client.disconnect();
	}
	} catch (MqttException e) {
	e.printStackTrace();
	}
	}
}

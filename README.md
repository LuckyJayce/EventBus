# EventBus
事件总线

#简介
事件总线的思路源于 https://github.com/greenrobot/EventBus  
不过代码和实现方式完全不同于EventBus.

greenrobot的EventBus是通过 onEvent的方式，然后定义Event实体类  

而本项目的代码是通过用户定义IEvent接口，然后程序通过动态代理实现接口，使用者通过这个实现类去调用接口的方法，直接通知注册并实现接口的注册者

#EventBus in 5 steps

1.定义事件接口，直接继承于IEvent	

	public interface IMessageEvent extends IEvent{ 
    	void onReceiveMessage(String message);
    }

2.注册监听
  
	EventBus.register(MainActivity.this);

3.注销监听

	EventBus.unregister(MainActivity.this);

4.实现事件

	 public MainActivity extends Activity implements IMessageEvent{
		 @Override
		 public void onReceiveMessage(String message){
	       
	    }
	 }

5.发送事件

	EventBus.get(IMessageEvent.class).onReceiveMessage("Message");

#Gradle引用
	
   提交jcenter中

#License

	Copyright 2016 LuckyJayce
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	   http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

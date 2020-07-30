package com.mmall.task;

import com.mmall.common.Const;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 这个类用于处理定时关单的类。
 （1）关于关单
我们在之前生成订单的时候，并没有实现定时关单，例如用户产生一个订单，如果30分钟内没有付款的话，
 我们需要将这个订单进行关闭，然后将下单这个产品的库存，再添加到数据库产品表中。

（2）由于我们使用的是Tomcat集群，因此在关闭订单的时候，需要使用分布式锁，即只在一个Tomcat中执行关闭订单的定时任务。
 这样就可以避免多个Tomcat都在执行关单任务，因为就可以减少系统消耗，避免多个Tomcat并发关单所造成的并发问题。
 （如果不使用分布式锁，多个Tomcat会同时执行关单任务，一方面浪费服务器性能；另一方面，多个并发任务同时执行关单，任意出现数据错乱（虽然我们执行加锁）
 */

@Slf4j
@Component //这个对象也要添加到 Spring容器
public class CloseOrderTask
{
    @Autowired
    private IOrderService iOrderService;

    //下面要调用 OrderServiceImpl 的方法closeOrder()，对订单进行关闭

    /**
     * 关闭订单版本1（无分布式）
     * 使用 Spring Schedule 的 @Scheduled(cron = "0 * /1 * * * ?")，这里的cron表达式表示每个一分钟的整数倍执行一次。
     */
//    @Scheduled(cron = "0 */1 * * * ?")
    public void closeOrderTaskV1()
    {
        log.info("关闭订单定时任务启动");
        //下面设置关单时间，设置默认值为2
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour" , "2"));
//        iOrderService.closeOrder(hour);
        log.info("关闭订单定时任务结束");
    }

    /**
     * 关闭订单版本2（分布式锁）
     * 原理：
     */
    @Scheduled(cron="0 */1 * * * ?")
    public void closeOrderTaskV2()
    {
        log.info("关闭订单定时任务启动");
        //1、设置分布式锁的超时时间（即这个分布式锁要锁多久，默认值是5000毫秒，即5秒）
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout","5000"));

        /**
        2、通过 RedisShardedPoolUtil，向redis分布式中设置一个分布式锁，它的时间是当前时间加上锁的有效时间（String类型），返回设置的结果
         （1）当第一个Tomcat进来的时候，先向redis设置一个锁，此时redis中没有锁，设置锁成功，那么此时就可以进行关单操作。
            后面如果有其他Tomcat进来，由于分布式redis中已经有锁，那么此时这个Tomcat便无法设置锁，也就无法执行关单操作。
         这就可以保证，在某一段时间内（分布式锁有效期），只有一个Tomcat在执行关单。

       （2）我们在 closeOrder 中会设置锁的有效时间，当这个锁的有效期到，或者业务执行完删除锁，此时其他的Tomcat就可以
            向redis中继续设置锁，其他Tomcat也有机会执行关单！当然，这里仍然保证同一时间段内（分布式锁有效期），只有一个Tomcat执行关单！

         （3）关于 分布式锁设置到redis分布式的那一个redis中，这是没有关系的，只要分布式的redis中有一个redis中保存有分布式锁，
            就说明整个分布式中有这个锁，其他的服务器就无法设置锁

         */
        Long setResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK , String.valueOf(System.currentTimeMillis()+lockTimeout));
        if(setResult!=null && setResult.intValue()==1)
        {
            //如果返回值是1，代表设置成功，获取锁，然后通过 closeOrder 方法进行关单操作
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }
        else
        {
            log.info("没有获得分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }

        log.info("关闭订单定时任务结束");
    }


//-------------------------------------------------------------------
    /**
     * 获取分布式锁，进行关单操作
     * @param lockName
     */
    private void closeOrder(String lockName)
    {
        //1、首先，我们在关单的时候系统可能发生故障，从而导致业务不会执行完，此时分布式锁无法释放，导致死锁，
        // 那么我们必须设置分布式锁的有效时间，即这个有效期到了，不管业务有没有执行完，都会释放锁，防止死锁
        RedisShardedPoolUtil.expire(lockName , 50);//有效期50秒，防止死锁
        log.info("获取{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());

        //2、进行关单
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour","2"));
//        iOrderService.closeOrder(hour);

        //3、业务执行完（即当前的订单删除完），马上删除锁，以便后面的进程能够进来，删除后面的无效订单
        RedisShardedPoolUtil.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        log.info("释放{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());

        log.info("===============================");
    }

}

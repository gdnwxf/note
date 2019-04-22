## beego框架源码分析

项目地址 <https://github.com/lhtzbj12/sdrms> 

**beego 入口函数**
beego.Run()

routers 实例化url 和 controller的mapping 映射关系

sysinit
	initDatabase.go  初始化数据库的信息

​	sysinit.go  初始化日志 缓存 数据库

![image-20190422231858918](/Users/wch/opensource/note/assets/image-20190422231858918.png)

```go
// ControllerInterface is an interface to uniform all controller handler.
type ControllerInterface interface {
   Init(ct *context.Context, controllerName, actionName string, app interface{})// 初始化参数
   Prepare() // 执行统一的额prepare方法
   Get()
   Post()
   Delete()
   Put()
   Head()
   Patch()
   Options()
   Finish()
   Render() error
   XSRFToken() string
   CheckXSRFCookie() bool
   HandlerFunc(fn string) bool
   URLMapping()
}
```








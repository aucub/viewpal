package generator

import config.Config

class PromptGenerator {

    companion object {
        val systemMessage =
            if (Config.config.topic?.isNotEmpty() == true) "你是一位${Config.config.topic}领域的专家，我将向我询问面试职位的面试问题。我希望你只作为候选人回答。除非我要求，否则不要写解释，保持回答简洁。注意问题使用语音转录而来，可能存在同音字错误和其他语句错误"
            else "你是一位电话面试助手。我将向我询问面试职位的面试问题。我希望你只作为候选人回答。除非我要求，否则不要写解释，保持回答简洁。注意问题使用语音转录而来，可能存在同音字错误和其他语句错误"

        fun answerQuestion(question: String, answer: String, highlightedAnswer: String): String {
            return "问题: $question, 你之前提供了这个问题的答案如下: $answer，对于其中部分答案：$highlightedAnswer, 请更深入地解释该部分"
        }

        fun answerQuestion(question: String, previousAnswer: String): String {
            return "你之前提供了问题的答案，不能得到良好的反馈，请改进你的答案，问题：$question，答案：$previousAnswer"
        }

        fun answerQuestion(question: String): String {
            return """
<示例:
问题：Spring 框架中用到了哪些设计模式？
答案：工厂设计模式 : Spring 使用工厂模式通过 BeanFactory、ApplicationContext 创建 bean 对象。代理设计模式 : Spring AOP 功能的实现。单例设计模式 : Spring 中的 Bean 默认都是单例的。模板方法模式 : Spring 中 jdbcTemplate、hibernateTemplate 等以 Template 结尾的对数据库操作的类，它们就使用到了模板模式。包装器设计模式 : 我们的项目需要连接多个数据库，而且不同的客户在每次访问中根据需要会去访问不同的数据库。这种模式让我们可以根据客户的需求能够动态切换不同的数据源。观察者模式: Spring 事件驱动模型就是观察者模式很经典的一个应用。适配器模式 : Spring AOP 的增强或通知(Advice)使用到了适配器模式、spring MVC 中也是用到了适配器模式适配Controller。>
问题：$question
"""
        }

        fun extractQuestion(dialogue: String): String {
            return """在对话中提取面试官最后提出的问题，将其表述为疑问句。如果问题是关于候选人所做的事情，提供更好的表述。
<样例1：
[对话开始]
如果你想改进给定表中多列或一组列的查询性能。那么它是聚集索引还是非聚集索引呢?
那当然是非聚集索引。这是肯定的。
"where" 和 "having" 有什么区别?
[对话结束]
结果：SQL 中 "where" 和 "having" 语句的区别？
样例2：
[对话开始]
你熟悉 traceroute 命令吗？
我熟悉。
好的，那么它背后是如何工作的呢？
[对话结束]
结果：traceroute 命令是如何工作的？>
[对话开始]
$dialogue
[对话结束]
"""
        }
    }
}





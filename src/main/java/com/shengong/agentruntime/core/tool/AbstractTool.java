package com.shengong.agentruntime.core.tool;

import com.shengong.agentruntime.core.param.ParamBinder;
import com.shengong.agentruntime.core.tool.annotation.ToolDefinition;
import com.shengong.agentruntime.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Tool 抽象基类
 * 提供了基于注解的元数据读取和参数绑定功能
 *
 * @author 神工团队
 * @since 1.1.0
 */
@Slf4j
public abstract class AbstractTool implements Tool {

    private final ToolDefinition definition;

    @Autowired
    protected ParamBinder paramBinder;

    protected AbstractTool() {
        this.definition = this.getClass().getAnnotation(ToolDefinition.class);
        if (this.definition == null) {
            throw new IllegalStateException("Tool class " + this.getClass().getName() +
                                         " must be annotated with @ToolDefinition");
        }
    }

    @Override
    public String name() {
        return definition.name();
    }

    @Override
    public String description() {
        return definition.description();
    }

    @Override
    public String category() {
        return definition.category();
    }
}

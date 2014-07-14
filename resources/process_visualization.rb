require 'erb'
require 'rubygems'
require 'pp'
require 'coderay'
include Java

##### Helpers ##########

VH = viewHelper

def removeIndent(previous, event)
  
  lines = VH.getSourceLines(previous, event)
  
  # count indentation to find smallest indentation
  count = 99999;
  lines.each do |line|
    match = line.match(/^\<span .+?span\>(\s+).+/)
      
    next if not match 
      
    line_count = match[1].length  
    
    count = line_count if line_count < count
  end
  
  # remove count spaces from start of each line
  new_lines = []
  
  lines.each do |line|
    match = line.match(/^\<span .+?span\>(\s+).+/)
      
    next if not match
    
    offsets = match.offset 1
    
    new_line = line[0..offsets[0]]

    new_line += line[offsets[0]+count..offsets[1]-1]
    
    # puts line[offsets[0]+count..offsets[1]-1]
    
    new_line += line[offsets[1]..-1]
        
    new_lines << new_line
  end
  
  new_lines
end

def threadClassTypes(previous, event, inSynchronized)
  name = previous.thread.name
  
  out = "thread_#{name} "
  
  if(previous.type == "synchronizedMethodEntered" or previous.type == "lockWaitEnd" or previous.type == "synchronizedBlockEntered")
    out += "thread_#{name}_synchronized_start "
  end
  
  if(inSynchronized[previous.thread.name])
    out += "thread_#{name}_synchronized "
  end
  
  if(event.type == "synchronizedMethodExit" or event.thread != previous.thread or event.type == "returnStatement" or event.type == "lockWaitStart" or event.type == "synchronizedBlockExit")
    out += "thread_#{name}_synchronized_end "
  end
  
  out
end

def eventIndent(event)
  if event.indent != -1
    return "indent_#{event.indent}_#{event.max_indent}"
  end
  
  return ""
end

def threadColorBox()
  out = ""
  VH.threadCount().times do |n|
    color = VH.threadColor(n)
    out += %Q(<div class="thread-color-box" style="border: 1px solid #{color}; color: #{color}">#{VH.threadName(n)}</div>)
  end
  out
end

def threadStyles()
  out = ""
  VH.threadCount().times do |n|
    color_rgb = VH.threadColor(n)
    color_event_background = VH.threadColorAlpha(n, "0.05")
    color_event_border = VH.threadColorAlpha(n, "0.4")
    name = VH.threadName(n)
    t = <<STYLE
    .thread_#{name}{
      background-color: #{color_event_background}; 
      border-top-color: #{color_event_border};
    }

    .thread_#{name}_synchronized{
      border-left: 3px solid #{color_rgb};
      border-right: 3px solid #{color_rgb};
    }


    .thread_#{name}_synchronized_start{
      border-top: 3px solid #{color_rgb};

      -webkit-border-top-left-radius: 10px;
      -webkit-border-top-right-radius: 10px;

      -moz-border-radius-topleft: 10px;
      -moz-border-radius-topright: 10px;
    }

    .thread_#{name}_synchronized_end{
      border-bottom: 3px solid #{color_rgb};
      
      -webkit-border-bottom-left-radius: 10px;
      -webkit-border-bottom-right-radius: 10px;
      
      -moz-border-radius-bottomleft: 10px;
      -moz-border-radius-bottomright: 10px;
    }
STYLE
  out += t
	end
	out
end

def objectStyles
  out = ""
  VH.getObjectOrder.each do |object|
    out += %Q(.object-column-#{object.oid} { width: 250px; })
  end
  
  out
end

def indentStyles
  out = ""
  
  VH.cssIndent.times do |n|
    n.times do |i|
      out += ".indent_#{i}_#{n} {}"
    end
  end
end

def checkSynchronizedIn(previous, event, inSynchronized)
  if not inSynchronized.has_key?(previous.thread.name) or not inSynchronized.has_key?(event.thread.name) 
    inSynchronized[previous.thread.name] = false
  end

  if(previous.type == "synchronizedMethodEntered" or previous.type == "synchronizedBlockEntered")
    inSynchronized[previous.thread.name] = true
  end
end

def checkSynchronizedOut(event, inSynchronized)
  if(event.type == "synchronizedMethodExit" or event.type == "synchronizedBlockExit" or event.type == "returnStatement")
    inSynchronized[event.thread.name] = false
  end
end

def eventDetails(previous, event)
out = <<EVENT
  <div class="event-detail">
    Entry Type:</br>#{previous.type}</br>
    Exit Type:</br>#{event.type}</br>

    Entry Index:</br>#{previous.index}</br>
    Exit Index:</br>#{event.index}</br>

    Entry Line:</br>#{previous.lineNo}</br>
    Exit Line:</br>#{event.lineNo}</br>

    Entry Position:</br>#{previous.position}</br>
    Exit Position:</br>#{event.position}</br>

  </div>
EVENT
end

def checkLooping(previous, event)
  if event.type == "loopIn" and previous.type == "loopOut" 
    previous = event
    return true
  else
    return false
  end
end

def checkExit(previous, event)
  if (event.type == "threadRunExit" or event.type == "programExit") and previous.type == "threadDeathException"
    return true
  else
    return false
  end
end

def eventStyle(previous, event, headerSpace)
  %Q(top: #{headerSpace + previous.position}px; height: #{event.position - previous.position}px;)
end

def eventClass(previous, event, inSynchronized)
  %Q(event event-#{VH.getBlockType(previous, event)} #{threadClassTypes(previous, event, inSynchronized)} #{objectIndent(previous)})
end

def objectIndent(previous)
  if(previous.indent != -1)
    %Q(indent_#{previous.indent}_#{previous.max_indent})
  else
    %Q(no_indent)
  end
end

# process template
template_string = ""

while (line = template.readLine) != nil
  template_string << line << "\n"
end

templateERB = ERB.new(template_string)

# objectNames = {}
# 
# objectIds.each do |oid, obj|
#   objectNames[oid] = obj
# end

# process sources
sources.each do |key, source|
  string = CodeRay.scan(source.sourceString, :java).html(:css => :class, :line_numbers => :inline)
  
  source.highlightedString = string
  #pp match[1].split("\n")
  source.setHighlighting()
end

out.append(templateERB.result(binding)) 


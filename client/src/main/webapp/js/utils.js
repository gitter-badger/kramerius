/**
 * Returns true (or false) if given keypath in given objects exists
 * @param {string} keys combined keys into one string.  '/' is used as delimiter. 
 * @global
 */
function lookUpKey(keypath, object) {
    var stack = [];
    var paths = keypath.split("/");
    stack.push(object);
    while (stack.length > 0) {
        if (paths.length == 0)
            break;
        var struct = stack.pop();
        var topkey = paths[0];
        paths.shift();
        var defined = (typeof struct[topkey] != 'undefined');
        if (defined) {
            stack.push(struct[topkey]);
        } else {
            return false;
        }
    }
    return paths.length == 0;

}

function link(addr) {
        window.location.assign(addr);
}

/**
 * Returns true if argument is empty string
 * @param {string} tested string 
 * @global
 */
function isEmpty(str) {
    return (!str || 0 === str.length);
}

/**
 * Mix two objects into one
 * @function
 * @global
 */
function mixInto(target, source, methodNames) {
    var args = Array.prototype.slice.apply(arguments);
    target = args.shift();
    source = args.shift();

    methodNames = args;

    if (methodNames.length == 0) {
        for (var k in source) {
            var type = typeof source[k];
            if (type === "function") {
                methodNames.push(k);
            }
        }
    }

    var method;
    var length = methodNames.length;

    for (var i = 0; i < length; i++) {
        method = methodNames[i];

        // bind the function from the source and assign the
        // bound function to the target
        target[method] = _.bind(source[method], source);
    }
    return target;
}

/** stacktrace utility */
function stacktrace() {
    function st2(f) {
        return !f ? [] :
                st2(f.caller).concat([f.toString().split('(')[0].substring(9) + '(' + f.arguments.join(',') + ')']);
    }
    return st2(arguments.callee.caller);
}

// Array Remove - By John Resig (MIT Licensed)
Array.prototype.remove = function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};

function colorToHex(c) {
    return ($.rgbHex(c)).toString().toUpperCase();
}


var Point = function(x, y) {
    this.x = x;
    this.y = y;
};

Point.prototype = {
    x: 0,
    y: 0,
    getX: function() {
        return this.x;
    },
    getY: function() {
        return this.y;
    },
    setX: function(x) {
        this.x = x;
    },
    setY: function(y) {
        this.y = y;
    },
    move: function(dx, dy) {
        this.x = this.x + dx;
        this.y = this.y + dy;
    }
}

var Size = function(w, h) {
    this.width = w;
    this.height = h;
};

Size.prototype = {
    width: 0,
    height: 0,
    getWidth: function() {
        return this.width;
    },
    getHeight: function() {
        return this.height;
    }
}

var Rectangle = function(x, y, width, height) {
    this.location = new Point(x, y);
    this.size = new Size(width, height);

    this.center = new Point(x + width / 2, y + height / 2);
};
Rectangle.prototype = {
    location: new Point(0, 0),
    size: new Size(0, 0),
    center: new Point(0, 0),
    getX: function() {
        return this.location.getX();
    },
    getY: function() {
        return this.location.getY();
    },
    getWidth: function() {
        return this.size.getWidth();
    },
    getHeight: function() {
        return this.size.getHeight();
    },
    getLocation: function() {
        return this.location;
    },
    getCenter: function() {
        return this.center;
    },
    getSize: function() {
        return this.size;
    },
    clone: function() {
        return new Rectangle(this.location, this.size);
    }
}


function isScrolledIntoView(elem, view) {
    var docViewTop = $(view).offset().top;
    var docViewBottom = docViewTop + $(view).height();

    var elemTop = $(elem).offset().top;
    var elemBottom = elemTop + $(elem).height();
    return ((elemBottom >= docViewTop) && (elemTop <= docViewBottom));
}

function isTouchDevice() {
    return (('ontouchstart' in window)
            || (navigator.MaxTouchPoints > 0)
            || (navigator.msMaxTouchPoints > 0));
}


/**
 * Close all opened panel
 * @function cleanWindow
 * @static
 */
function cleanWindow() {
        $(".showing").each(function(index,value) {
                close(value);                                               
        });
}

/**
 * Open window defined by element selector
 * @function divopen
 * @static
 */
function divopen(elm) {
        $(elm).addClass("showing");        
        $(elm).show();        
}

/**
 * Close window defined by element selector
 * @function close
 * @static
 */
function close(elm) {
        $(elm).removeClass("showing");        
        $(elm).hide();        
}


function toggle(elm) {
        if ($(elm).is(':visible')) close(elm);                
        else divopen(elm);
}

function visible(elm) {
        return ($(elm).is(':visible'));                
}


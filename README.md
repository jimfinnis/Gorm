Gorm
====

Gorm is a Minecraft Bukkit plugin which generates enormous insane
castles while you watch.

Commands:

    /build      perform one build step. If there is no castle, will create a new "base
                courtyard" around the player. Subsequent builds will generate new buildings.
            
    /startgorm  start a task automatically doing "/build" every few seconds.

    /stopgorm   stop the gorm autobuild task
    
    /gt <str>   issue a test turtle string. There must be a castle for this.
    
Note that Gorm does not save rooms between reloads.
    



The turtle
----------

Much of the code uses the Turtle class, either directly:

    Turtle t = new Turtle(...)
    t.setMaterial(Material.SMOOTH_BRICK);
    t.up();
    t.write();
    t.forwards();
    t.write();
    t.turn(1);
    
or using "turtle language" strings, such as 

    m1wu.m2wu.m1wu.m2w.Mt.fwbbwfLwRRw
   
which builds a 3-high pillar with four torches on the top.

Instructions:

    .   no operation, used to break the string up for readability
    f   forwards. Will then write if WRITEONMOVE is set.
        If FOLLOW is set, will turn to follow walls
        If SUPPORTFOLLOW is set, will turn to always try to keep a block underneath
        If HUGEDGE is set, will avoid going into empty space by following an *outside* wall
        If LEFT is set, will try to turn left first
            If RANDOM is set, will try to turn randomly
                Default is turn right first
        If a turn occurred, the lastMoveCausedTurn condition will be set

    L   sidestep left - turn left, then do "f", then turn right
    R   sidestep right - turn right, then do "f", then turn left
    b   backstep - turn twice, do "f", then turn twice
    l   turn left without moving
    r   turn right without moving
    u   go up - if WRITEONMOVE is set, then write
    d   go down - if WRITEONMOVE is set, then write
    w   write the current material to the current block. If CHECKWRITE is set and this
        would overwrite a man-made block, do not write, and abort.
    t   test the block - if it is empty, add 1 to the test score
    T   test the block - if it is not empty, abort
    B   add a permanent block (will NOT work if setRoom has not been called,
        i.e. we are not placing furniture.) Will not work in test mode,
        or if the block is not empty and checkwrite is on.
    +   set a flag
            f   FOLLOW
            s   SUPPORTFOLLOW
            h   HUGEDGE
            l   LEFT
            ?   RANDOM
            w   WRITEONMOVE
            c   CHECKWRITE
            S   BACKSTAIRS (writes stairs facing away from us, rather than towards)
            t   TEST (do not write, just test we can - and abort if we can't)
            i   NOTINDOORS (will not write if it would write into the extent of an inside room)
            I   ABORTINDOORS (an attempt to write will abort if it would write into the extent of an inside room)
    -   clear a flag
    :   mark the repeat point - we loop back here at the end of the string, until we abort
    ?x  check the condition 'x', where x is:
            t   lastMoveCausedTurn
    (   if the condition was false, jump forwards to ")" (note this doesn't nest!)
    )   end of a jump block
    mx  set material from material manager, where x is:
        1   primary building material
        2   secondary building material
        3   secondary building material, requires support (e.g. gravel)
        f   fence material
        s   stair material
        S   primary material converted to stair (for architectural purposes)
        o   ornamental block
        g   ground
        p   pole (e.g fencepost or cobble post but not iron bar)
        The actual materials are set randomly for each building.
    Mx  set material directly, where x is:
        w   wood
        q   wall sign, facing towards player
        j   wooden stairs
        f   wooden fence
        W   cobble fence
        l   log
        b   stone brick
        c   chiselled stone brick
        s   stone stairs
        L   lapis (for debugging)
        t   torch
        a   air
        g   glass pane
        G   glass block
        i   iron bars
        B   bookshelf
        D   bed
        0-9 custom writer 0-9
        Try to avoid this and use 'm' instead

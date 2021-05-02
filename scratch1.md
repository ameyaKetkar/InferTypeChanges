

List -> Set 

statement 1. `lines = new ArrayList<>();`

statement 2. `lines = new HashSet<>();`

---------
statement 1. `lines = new ArrayList<>();`
            Assignment- :[31]:[[32]]:[33c~\s*]=:[34]

statement 2. `lines = new HashSet<>();`
            Assignment- :[31]:[[32]]:[33c~\s*]=:[34]
            

`new ArrayList<>();`  new :[[3]]<:[4]>(:[5])
3: ArrayList, 4:<>  , 5: <>


`new HashSet<>()`  new :[[3]]<:[4]>(:[5])
3: HashSet, 4:<>  , 5: <>


new :[[3]]<:[4]>(:[5]) -> `new ArrayList<:[4](:[5])>`

new :[[3]]<:[4]>(:[5]) -> `new HashSet<:[4](:[5])>`

-> .. config file


x + 5 ->      :[1] + :[2]

x.add(5) ->  :[1].add(:[2])
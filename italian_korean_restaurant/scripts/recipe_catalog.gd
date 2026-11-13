extends RefCounted
class_name RecipeCatalog

# Each recipe has a cuisine, ingredients, station and preparation duration.
const RECIPES: Array[Dictionary] = [
	{"name":"Margherita Pizza","cuisine":"ITALIAN","ingredients":["dough","tomato","mozzarella","basil"],"station":"Pizza oven","time":8},
	{"name":"Pepperoni Pizza","cuisine":"ITALIAN","ingredients":["dough","tomato","mozzarella","pepperoni"],"station":"Pizza oven","time":9},
	{"name":"Quattro Formaggi","cuisine":"ITALIAN","ingredients":["dough","mozzarella","gorgonzola","parmesan"],"station":"Pizza oven","time":10},
	{"name":"Diavola Pizza","cuisine":"ITALIAN","ingredients":["dough","tomato","salami","chili"],"station":"Pizza oven","time":9},
	{"name":"Prosciutto Pizza","cuisine":"ITALIAN","ingredients":["dough","tomato","mozzarella","prosciutto"],"station":"Pizza oven","time":10},
	{"name":"Carbonara","cuisine":"ITALIAN","ingredients":["spaghetti","egg","pecorino","guanciale"],"station":"Pasta range","time":7},
	{"name":"Pasta Bolognese","cuisine":"ITALIAN","ingredients":["tagliatelle","beef ragù","tomato","parmesan"],"station":"Pasta range","time":8},
	{"name":"Pesto Linguine","cuisine":"ITALIAN","ingredients":["linguine","basil pesto","pine nuts","parmesan"],"station":"Pasta range","time":7},
	{"name":"Arrabbiata","cuisine":"ITALIAN","ingredients":["penne","tomato","garlic","chili"],"station":"Pasta range","time":7},
	{"name":"Alfredo Fettuccine","cuisine":"ITALIAN","ingredients":["fettuccine","cream","butter","parmesan"],"station":"Pasta range","time":8},
	{"name":"Lasagna","cuisine":"ITALIAN","ingredients":["pasta sheets","beef ragù","béchamel","parmesan"],"station":"Oven","time":12},
	{"name":"Eggplant Parmigiana","cuisine":"ITALIAN","ingredients":["eggplant","tomato","mozzarella","basil"],"station":"Oven","time":10},
	{"name":"Mushroom Risotto","cuisine":"ITALIAN","ingredients":["arborio rice","mushroom","stock","parmesan"],"station":"Risotto range","time":11},
	{"name":"Seafood Risotto","cuisine":"ITALIAN","ingredients":["arborio rice","shrimp","mussels","stock"],"station":"Risotto range","time":12},
	{"name":"Gnocchi Sorrentina","cuisine":"ITALIAN","ingredients":["potato gnocchi","tomato","mozzarella","basil"],"station":"Pasta range","time":8},
	{"name":"Ravioli Ricotta","cuisine":"ITALIAN","ingredients":["ravioli","ricotta","sage","butter"],"station":"Pasta range","time":8},
	{"name":"Chicken Parmigiana","cuisine":"ITALIAN","ingredients":["chicken","breadcrumbs","tomato","mozzarella"],"station":"Fryer","time":10},
	{"name":"Bruschetta","cuisine":"ITALIAN","ingredients":["bread","tomato","garlic","olive oil"],"station":"Prep counter","time":4},
	{"name":"Caprese Salad","cuisine":"ITALIAN","ingredients":["tomato","mozzarella","basil","olive oil"],"station":"Prep counter","time":4},
	{"name":"Tiramisu","cuisine":"ITALIAN","ingredients":["mascarpone","espresso","ladyfingers","cocoa"],"station":"Dessert counter","time":6},
	{"name":"Classic Kimbap","cuisine":"KOREAN","ingredients":["rice","seaweed","egg","carrot","spinach"],"station":"Rice counter","time":7},
	{"name":"Bulgogi Kimbap","cuisine":"KOREAN","ingredients":["rice","seaweed","bulgogi","carrot","pickled radish"],"station":"Rice counter","time":8},
	{"name":"Tuna Kimbap","cuisine":"KOREAN","ingredients":["rice","seaweed","tuna","mayonnaise","cucumber"],"station":"Rice counter","time":7},
	{"name":"Bibimbap","cuisine":"KOREAN","ingredients":["rice","beef","spinach","carrot","gochujang"],"station":"Hot plate","time":9},
	{"name":"Dolsot Bibimbap","cuisine":"KOREAN","ingredients":["rice","beef","vegetables","egg","gochujang"],"station":"Stone bowl","time":10},
	{"name":"Kimchi Fried Rice","cuisine":"KOREAN","ingredients":["rice","kimchi","egg","scallion","sesame oil"],"station":"Wok","time":8},
	{"name":"Bulgogi","cuisine":"KOREAN","ingredients":["beef","soy sauce","pear","garlic","sesame"],"station":"Grill","time":9},
	{"name":"Dakgalbi","cuisine":"KOREAN","ingredients":["chicken","gochujang","cabbage","sweet potato"],"station":"Hot plate","time":11},
	{"name":"Korean Fried Chicken","cuisine":"KOREAN","ingredients":["chicken","potato starch","gochujang","honey"],"station":"Fryer","time":11},
	{"name":"Yangnyeom Chicken","cuisine":"KOREAN","ingredients":["chicken","chili paste","garlic","honey","sesame"],"station":"Fryer","time":11},
	{"name":"Tteokbokki","cuisine":"KOREAN","ingredients":["rice cakes","gochujang","fish cake","scallion"],"station":"Sauce pot","time":8},
	{"name":"Rabokki","cuisine":"KOREAN","ingredients":["ramen","rice cakes","gochujang","fish cake"],"station":"Sauce pot","time":9},
	{"name":"Kimchi Jjigae","cuisine":"KOREAN","ingredients":["kimchi","pork","tofu","scallion","stock"],"station":"Soup range","time":10},
	{"name":"Doenjang Jjigae","cuisine":"KOREAN","ingredients":["soybean paste","tofu","zucchini","mushroom"],"station":"Soup range","time":10},
	{"name":"Jajangmyeon","cuisine":"KOREAN","ingredients":["noodles","black bean paste","pork","onion"],"station":"Noodle range","time":9},
	{"name":"Jjamppong","cuisine":"KOREAN","ingredients":["noodles","seafood","chili oil","cabbage"],"station":"Noodle range","time":11},
	{"name":"Japchae","cuisine":"KOREAN","ingredients":["glass noodles","beef","spinach","carrot","soy"],"station":"Wok","time":8},
	{"name":"Mandu","cuisine":"KOREAN","ingredients":["dumpling wrappers","pork","tofu","cabbage"],"station":"Steamer","time":9},
	{"name":"Hotteok","cuisine":"KOREAN","ingredients":["flour dough","brown sugar","cinnamon","peanuts"],"station":"Dessert griddle","time":7},
	{"name":"Bingsu","cuisine":"KOREAN","ingredients":["shaved ice","red bean","condensed milk","fruit"],"station":"Dessert counter","time":5}
]

static func random_recipe() -> Dictionary:
	return RECIPES[randi() % RECIPES.size()]

static func cuisine_count(cuisine: String) -> int:
	var total := 0
	for recipe in RECIPES:
		if recipe.cuisine == cuisine:
			total += 1
	return total

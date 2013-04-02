$(function() {
	switch($.urlParam('action')) {
	case 'create':
		switch($.urlParam('status')) {
		case 'success':
			window.noty({text: 'Produto incluido com sucesso', type: 'success', timeout: 2000});
		}
		break;
	case 'update':
		switch($.urlParam('status')) {
		case 'success':
			window.noty({text: 'Produto alterado com sucesso', type: 'success', timeout: 2000});
		}
		break;
	}
	
	var Categoria = Backbone.Model.extend({
		idAttribute: "descricao",
		urlRoot: '/rest/categorias'
	});
	
	var Categorias = Backbone.Collection.extend({
		url: '/rest/categorias',
		model: Categoria
	});
	
	var CategoriaEditActionView = Backbone.View.extend({
		tagName: 'button',
		events: {
			'click': 'onAction'
		},
		initialize: function() {
			_.bindAll(this, 'render', 'onAction');
		},
		render: function() {
			this.$el.text('Editar').button();
			return this;
		},
		onAction: function(e) {
			e.preventDefault();
			window.location = '/categoria.xhtml?action=update&categoria=' + encodeURIComponent(this.model.get('descricao'));
		}
	});
	
	var CategoriaDeleteActionView = Backbone.View.extend({
		tagName: 'button',
		events: {
			'click': 'onAction'
		},
		initialize: function() {
			_.bindAll(this, 'render', 'onAction');
		},
		render: function() {
			this.$el.text('Excluir').button();
			return this;
		},
		onAction: function(e) {
			e.preventDefault();
			
			var categoria = this.model;
			var excluirCategoria = function($noty) {
				categoria.destroy({
					wait: true,
					success: function(model, response, options) {
						$noty.close();
						noty({text: 'Categoria excluida com sucesso', type: 'success', timeout: 2000});
					},
					error: function(model, xhr, options) {
						$noty.close();
						noty({text: 'Falha na exclusão da categoria', type: 'error'});
					}
				});
	      	};
	      	
			noty({
				text: 'Deseja realmente excluir a categoria?',
				layout: 'center',
				buttons: [
				    {
					    addClass: 'btn',
					    text: 'Cancelar',
					    onClick: function($noty) {
					        $noty.close();
				    	}
				 	},
					{
						addClass: 'btn', 
						text: 'Confirmar', 
						onClick: excluirCategoria
				    }
				],
				callback: {
					onShow: function() {
						$('button.btn').button();
					}
				}
			});
		}
	});
	
	var CategoriaRowView = Backbone.View.extend({
		tagName: 'tr',
		initialize: function() {
			_.bindAll(this, 'render');
		},
		render: function() {
			var tdCol1 = $('<td/>', {text: this.model.get('descricao')}).appendTo(this.$el);
			var tdCol2 = $('<td/>').appendTo(this.$el);
			
			new CategoriaEditActionView({model: this.model}).render().$el.appendTo(tdCol2);
			new CategoriaDeleteActionView({model: this.model}).render().$el.appendTo(tdCol2);
			
			return this;
		}
	});
	
	var CategoriasView = Backbone.View.extend({
		el: $('body'),
		events: {
			'click #adicionar': 'adicionar'
		},
		initialize: function() {
			_.bindAll(this, 'render', 'adicionar');
			
			this.collection = new Categorias();
			this.listenTo(this.collection, 'add', this.render);
			this.listenTo(this.collection, 'remove', this.render);
			this.listenTo(this.collection, 'sync', this.render);
			this.collection.fetch();
		},
		render: function() {
			this.$el.empty();
			
			var form = $('<form/>').appendTo(this.$el);
			var header = $('<header/>', {text: 'Categorias'}).appendTo(form);
			var fieldset = $('<fieldset/>').appendTo(form);
			
			var table = $('<table/>', {id: 'table:categorias'}).appendTo(fieldset);
			var thead = $('<thead/>').appendTo(table);
			var trHead = $('<tr/>').appendTo(thead);
			var tdCol1 = $('<td/>', {text: 'Descrição'}).appendTo(trHead);
			var tdCol2 = $('<td/>', {text: 'Ações'}).appendTo(trHead);
			
			var tbody = $('<tbody/>').appendTo(table);
			this.collection.each(function(categoria) {
				new CategoriaRowView({model: categoria}).render().$el.appendTo(tbody);
			});
			
			var button = $('<button/>', {id: 'adicionar', text: 'Adicionar'}).button().appendTo(fieldset);
			
			return this;
		},
		adicionar: function(e) {
			e.preventDefault();
			window.location = '/categoria.xhtml?action=create';
		}
	});
	
	new CategoriasView();
});
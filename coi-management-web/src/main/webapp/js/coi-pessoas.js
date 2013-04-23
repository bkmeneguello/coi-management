COI.module("Pessoas", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Pessoa = Backbone.Model.extend({
		defaults: {
			nome: null,
			codigo: null
		}
	});

	var Pessoas = Backbone.Collection.extend({
		url: '/rest/pessoas',
		model: Pessoa
	});
	
	var PessoaRowView = Marionette.ItemView.extend({
		template: '#pessoa_row_template',
		tagName: 'tr',
		events: {
			'click .coi-action-update': 'doUpdate',
			'click .coi-action-delete': 'doDelete'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.$('button').button();
		},
		doUpdate: function(e) {
			e.preventDefault();
			Backbone.history.navigate('pessoa/' + this.model.get('id'), true);
		},
		doDelete: function(e) {
			e.preventDefault();
			
			var model = this.model;
			_promptDelete(function() {
				model.destroy({
					wait: true,
					success: function(model, response, options) {
						_notifyDelete();
					},
					error: function(model, xhr, options) {
						_notifyDeleteFailure();
					}
				});
			});
		}
	});
	
	var PessoasView = Marionette.CompositeView.extend({
		template: '#pessoas_template',
		tagName: 'form',
		itemViewContainer: 'tbody',
		itemView: PessoaRowView,
		events: {
			'click .coi-action-cancel': 'doCancel',
			'click .coi-action-create': 'doCreate'
		},
		ui: {
			'table': 'table'
		},
		initialize: function() {
			_.bindAll(this);
			this.collection.fetch();
		},
		onRender: function() {
			this.$el.form();
			this.ui.table.table().css('width', '100%');
		},
		doCancel: function(e) {
			e.preventDefault();
			Backbone.history.navigate('', true);
		},
		doCreate: function(e) {
			e.preventDefault();
			Backbone.history.navigate('pessoa', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pessoas', function() {
			COI.body.show(new PessoasView({collection: new Pessoas()}));
		});
	});
});